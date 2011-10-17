package sbtfilter

import sbt._
import Keys._
import Defaults._
import Project.Setting
import collection.JavaConversions._
import java.io.File

object Plugin extends sbt.Plugin {
  import FilterKeys._

  object FilterKeys {
    val filterDirectoryName = SettingKey[String]("filter-directory-name", "Default filter directory name.")
    val filterDirectory = SettingKey[File]("filter-directory", "Default filter directory, used for filters.")
    val filters = TaskKey[Seq[File]]("filters", "All filters.")
    val extraProps = SettingKey[Seq[(String, String)]]("filter-extra-props", "Extra filter properties.")
    val projectProps = TaskKey[Seq[(String, String)]]("filter-project-props", "Project filter properties.")
    val systemProps = TaskKey[Seq[(String, String)]]("filter-system-props", "System filter properties.")
    val managedProps = TaskKey[Seq[(String, String)]]("filter-managed-props", "Managed filter properties.")
    val unmanagedProps = TaskKey[Seq[(String, String)]]("filter-unmanaged-props", "Filter properties defined in filters.")
    val props = TaskKey[Seq[(String, String)]]("filter-props", "All filter properties.")
    val filterResources = TaskKey[Seq[(File, File)]]("filter-resources", "Filters all resources.")
  }

  lazy val filterConfigPaths: Seq[Setting[_]] = Seq(
    filterDirectory <<= (sourceDirectory, filterDirectoryName) apply { (d, name) => d / name },
    sourceDirectories in filters <<= Seq(filterDirectory).join,
    filters <<= collectFiles(sourceDirectories in filters, includeFilter in filters, excludeFilter in filters))
  lazy val filterConfigTasks: Seq[Setting[_]] = Seq(
    filterResources <<= filterTask triggeredBy copyResources,
    copyResources in filterResources <<= (copyResources).identity,
    managedProps <<= (projectProps, systemProps) map (_ ++ _),
    unmanagedProps <<= unmanagedPropsTask,
    props <<= (extraProps, managedProps, unmanagedProps) map (_ ++ _ ++ _))
  lazy val filterConfigSettings: Seq[Setting[_]] = filterConfigTasks ++ filterConfigPaths
  
  lazy val baseFilterSettings = Seq(
    filterDirectoryName := "filters",
    extraProps := Nil,
    projectProps <<= projectPropsTask,
    systemProps <<= (state) map { (state) => SystemProps() },
    includeFilter in filters := "*.properties" | "*.xml",
    excludeFilter in filters := HiddenFileFilter,
    includeFilter in filterResources := "*.properties" | "*.xml",
    excludeFilter in filterResources := HiddenFileFilter || ImageFileFilter)
  lazy val filterSettings = baseFilterSettings ++ inConfig(Compile)(filterConfigSettings) ++ inConfig(Test)(filterConfigSettings)

  def projectPropsTask =
    (organization, name, description, version, scalaVersion, sbtVersion) map {
      (organization, name, desc, version, scalaVersion, sbtVersion) =>
        Seq(
          "organization" -> organization,
          "name" -> name,
          "description" -> desc,
          "version" -> version,
          "scalaVersion" -> scalaVersion,
          "sbtVersion" -> sbtVersion)
    }

  def unmanagedPropsTask =
    (streams, filters) map {
      (streams, filters) =>
        (Seq.empty[(String, String)] /: filters) { (acc, rf) => acc ++ Props(streams.log, rf) }
    }

  def filterTask =
    (streams, copyResources in filterResources, includeFilter in filterResources, excludeFilter in filterResources, props) map {
      (streams, resources, incl, excl, filterProps) =>
        val props = Map.empty[String, String] ++ filterProps
        val filtered = resources filter (r => incl.accept(r._1) && !excl.accept(r._1))
        Filter(streams.log, filtered map (_._2), props)
        resources
    }

  object ImageFileFilter extends FileFilter {
    val formats = Seq("jpg", "jpeg", "png", "gif", "bmp")
    def accept(file: File) = formats.exists(_ == file.ext.toLowerCase)
  }

  object SystemProps {
    def apply() = System.getProperties.stringPropertyNames.toSeq map (k => k -> System.getProperty(k))
  }

  object Props {
    import java.util.Properties

    def apply(log: Logger, path: File) = {
      val props = new Properties
      IO.load(props, path)
      props
    }
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
