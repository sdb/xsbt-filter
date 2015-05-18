package sbtfilter

import sbt._
import Keys._
import Defaults.collectFiles
import java.io.File
import java.util.Properties
import scala.collection.JavaConversions._

/* TODO
- configurable variable delimiters
- support web plugin
- watchSources
*/
trait FilterKeys {
  val filterDirectoryName  = settingKey[String]("Default filter directory name.")
  val filterDirectory      = settingKey[File]("Default filter directory, used for filters.")
  val filters              = taskKey[Seq[File]]("All filters.")
  val filterExtraProps     = settingKey[Seq[(String, String)]]("Extra filter properties.")
  val filterProjectProps   = taskKey[Seq[(String, String)]]("Project filter properties.")
  val filterSystemProps    = taskKey[Seq[(String, String)]]("System filter properties.")
  val filterEnvProps       = taskKey[Seq[(String, String)]]("Environment filter properties.")
  val filterManagedProps   = taskKey[Seq[(String, String)]]("Managed filter properties.")
  val filterUnmanagedProps = taskKey[Seq[(String, String)]]("Filter properties defined in filters.")
  val filterProps          = taskKey[Seq[(String, String)]]("All filter properties.")
  val filterResources      = taskKey[Seq[(File, File)]]("Filters all resources.")
}

object FilterPlugin extends AutoPlugin {
  import FileFilter.globFilter

  object autoImport extends FilterKeys

  import autoImport._

  override def requires = plugins.JvmPlugin

  override lazy val projectSettings = filterSettings

  lazy val filterResourcesTask = filterResources <<= filter(copyResources, filterResources) triggeredBy copyResources

  def filter(resources: TaskKey[Seq[(File, File)]], task: TaskKey[Seq[(File, File)]]) =
    (streams, resources in task, includeFilter in task, excludeFilter in task, filterProps) map {
      (streams, resources, incl, excl, filterProps) =>
        val props = Map.empty[String, String] ++ filterProps
        val filtered = resources filter (r => incl.accept(r._1) && !excl.accept(r._1) && !r._1.isDirectory)
        Filter(streams.log, filtered map (_._2), props)
        resources
    }

  lazy val projectPropsTask = filterProjectProps <<= (organization, name, description, version, scalaVersion, sbtVersion) map {
    (o, n, d, v, scv, sv) =>
      Seq("organization" -> o, "name" -> n, "description" -> d, "version" -> v, "scalaVersion" -> scv, "sbtVersion" -> sv)
  }

  lazy val unmanagedPropsTask = filterUnmanagedProps <<= (streams, filters) map {
    (streams, filters) => (Seq.empty[(String, String)] /: filters) { (acc, rf) => acc ++ properties(streams.log, rf) }
  }

  lazy val filterConfigPaths: Seq[Setting[_]] = Seq(
    filterDirectory <<= (sourceDirectory, filterDirectoryName) apply { (d, name) => d / name },
    sourceDirectories in filters <<= Seq(filterDirectory).join,
    filters <<= collectFiles(sourceDirectories in filters, includeFilter in filters, excludeFilter in filters),
    includeFilter in filters := "*.properties" | "*.xml",
    excludeFilter in filters := HiddenFileFilter,
    includeFilter in filterResources := "*.properties" | "*.xml",
    excludeFilter in filterResources := HiddenFileFilter || ImageFileFilter)

  lazy val filterConfigTasks: Seq[Setting[_]] = Seq(
    filterResourcesTask,
    copyResources in filterResources <<= copyResources,
    filterManagedProps <<= (filterProjectProps, filterSystemProps, filterEnvProps) map (_ ++ _ ++ _),
    unmanagedPropsTask,
    filterProps <<= (filterExtraProps, filterManagedProps, filterUnmanagedProps) map (_ ++ _ ++ _))

  lazy val filterConfigSettings: Seq[Setting[_]] = filterConfigTasks ++ filterConfigPaths

  lazy val baseFilterSettings = Seq(
    filterDirectoryName := "filters",
    filterExtraProps := Nil,
    projectPropsTask,
    filterEnvProps := sys.env.toSeq,
    filterSystemProps := sys.props.toSeq)

  lazy val filterSettings = baseFilterSettings ++ inConfig(Compile)(filterConfigSettings) ++ inConfig(Test)(filterConfigSettings)

  object ImageFileFilter extends FileFilter {
    val formats = Seq("jpg", "jpeg", "png", "gif", "bmp")
    def accept(file: File) = formats.exists(_ == file.ext.toLowerCase)
  }

  def properties(log: Logger, path: File) = {
    val props = new Properties
    IO.load(props, path)
    props
  }

  object Filter {
    import util.matching.Regex._
    import java.io.{ FileReader, BufferedReader, PrintWriter }

    val pattern = """((?:\\?)\$\{.+?\})""".r
    def replacer(props: Map[String, String]) = (m: Match) => {
      m.matched match {
        case s if s.startsWith("\\") => Some("""\$\{%s\}""" format s.substring(3, s.length -1))
        case s => props.get(s.substring(2, s.length -1))
      }
    }
    def filter(line: String, props: Map[String, String]) = pattern.replaceSomeIn(line, replacer(props))

    def apply(log: Logger, files: Seq[File], props: Map[String, String]) {
      log debug ("Filter properties: %s" format (props.mkString("{", ", ", "}")))
      IO.withTemporaryDirectory { dir =>
        files foreach { src =>
          log debug ("Filtering %s" format src.absolutePath)
          val dest = new File(dir, src.getName)
          val out = new PrintWriter(dest)
          val in = new BufferedReader(new FileReader(src))
          IO.foreachLine(in) { line => IO.writeLines(out, Seq(filter(line, props))) }
          in.close()
          out.close()
          IO.copyFile(dest, src, true)
        }
      }
    }
  }
}
