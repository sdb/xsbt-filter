import sys.error

version := "0.1"

name := "simple"

// Since SBT 0.13.6, `enablePlugins(FilterPlugin)` can be written directly
val root = (project in file(".")).
    enablePlugins(FilterPlugin).
    settings(filterExtraProps += "app.homepage" -> "http://localhost")

TaskKey[Unit]("check-compile") <<= (classDirectory in Compile) map { (cd) =>
  val props = new java.util.Properties
  IO.load(props, cd / "sample.properties")
  if (props.getProperty("name") != "simple")
    error("property not substituted")
  if (props.getProperty("homepage") != "http://localhost")
    error("property not substituted")
  if (props.getProperty("anothername") != "${project.name}")
    error("property substituted")
  if (IO.read(cd / "sample.txt") != "This ${project.name} shouldn't be substituted.\n")
    error("file filtered")
  ()
}

TaskKey[Unit]("check-test") <<= (classDirectory in Test) map { (cd) =>
  val props = new java.util.Properties
  IO.load(props, cd / "sample.properties")
  if (props.getProperty("name") != "simple")
    error("property not substituted")
  ()
}
