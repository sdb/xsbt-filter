version := "0.4-SNAPSHOT"

sbtPlugin := true

name := "xsbt-filter"

organization := "com.github.sdb"

seq(ScriptedPlugin.scriptedSettings: _*)

scalaSource in Compile <<= baseDirectory { (base) => base / "src" }

sbtTestDirectory <<= baseDirectory { (base) => base / "sbt-test" }

crossScalaVersions := Seq("2.9.1", "2.9.2")

scalacOptions ++= Seq("-deprecation", "-unchecked")

licenses := Seq("New BSD License" -> url("http://opensource.org/licenses/BSD-3-Clause"))

homepage := Some(url("http://sdb.github.com/xsbt-filter/"))

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) 
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else                             
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := (
  <scm>
    <url>git@github.com:sdb/xsbt-filter.git</url>
    <connection>scm:git:git@github.com:sdb/xsbt-filter.git</connection>
  </scm>
  <developers>
    <developer>
      <id>sdb</id>
      <name>Stefan De Boey</name>
      <url>https://github.com/sdb</url>
    </developer>
  </developers>
)