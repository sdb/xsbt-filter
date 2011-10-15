import sbt._
import Keys._
import Defaults._
import Project.Setting
import collection.JavaConversions._
import java.io.File

object FilterPlugin extends Plugin {
  val filterDirectoryName = SettingKey[String]("filter-directory-name", "Default filter directory name.")
  val filterDirectory = SettingKey[File]("filter-directory", "Default filter directory, used for resource filter files.")
  val filterDirectories = SettingKey[Seq[File]]("filter-directories", "Filter directories, used for resource filter files.")
  val filterIncludes = SettingKey[FileFilter]("filter-includes", "Filter for including resource filter files.")
  val filterExcludes = SettingKey[FileFilter]("filter-excludes", "Filter for excluding resource filter files.")
  val resourceFilters = TaskKey[Seq[File]]("resource-filters", "All resource filter files.")
  val extraFilterProps = SettingKey[Seq[(String, String)]]("extra-filter-props", "Extra filter properties.")
  val projectFilterProps = TaskKey[Seq[(String, String)]]("project-filter-props", "Project filter properties.")
  val systemFilterProps = TaskKey[Seq[(String, String)]]("system-filter-props", "System filter properties.")
  val managedFilterProps = TaskKey[Seq[(String, String)]]("managed-filter-props", "Managed filter properties.")
  val unmanagedFilterProps = TaskKey[Seq[(String, String)]]("unmanaged-filter-props", "Filter properties defined in resource filter files.")
  val filterProps = TaskKey[Seq[(String, String)]]("filter-props", "All filter properties.")
  val filteredIncludes = SettingKey[FileFilter]("filtered-includes", "Filter for including resources in filtering.")
  val filteredExcludes = SettingKey[FileFilter]("filtered-excludes", "Filter for excluding resources from filtering.")

  lazy val filterConfigPaths: Seq[Setting[_]] = Seq(
    filterDirectory <<= (sourceDirectory, filterDirectoryName) apply { (d, name) => d / name },
    filterDirectories <<= Seq(filterDirectory).join,
    resourceFilters <<= collectFiles(filterDirectories, filterIncludes in resourceFilters, filterExcludes in resourceFilters))
  lazy val filterConfigTasks: Seq[Setting[_]] = Seq(
    copyResources <<= filterTask,
    managedFilterProps <<= (projectFilterProps, systemFilterProps) map (_ ++ _),
    unmanagedFilterProps <<= unmanagedPropsTask,
    filterProps <<= (extraFilterProps, managedFilterProps, unmanagedFilterProps) map (_ ++ _ ++ _))
  lazy val filterConfigSettings: Seq[Setting[_]] = filterConfigTasks ++ filterConfigPaths
  
  lazy val baseFilterSettings = Seq(
    filterDirectoryName := "filters",
    extraFilterProps := Nil,
    projectFilterProps <<= projectPropsTask,
    systemFilterProps <<= (state) map { (state) => SystemProps() },
    filterIncludes := "*.properties" | "*.xml",
    filterExcludes := HiddenFileFilter,
    filteredIncludes := "*.properties" | "*.xml",
    filteredExcludes := HiddenFileFilter || ImageFileFilter)
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
    (streams, resourceFilters) map {
      (streams, filters) =>
        (Seq.empty[(String, String)] /: filters) { (acc, rf) => acc ++ Props(streams.log, rf) }
    }

  def filterTask =
    (streams, copyResources, filteredIncludes, filterExcludes, filterProps) map {
      (streams, resources, incl, excl, filterProps) =>
        val props = Map.empty[String, String] ++ filterProps
        val filtered = resources filter (r => incl.accept(r._1) && !excl.accept(r._1)) // TODO: see collectFiles
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
