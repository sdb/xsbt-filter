sbtPlugin := true

name := "xsbt-filter"

organization := "com.github.sdb"

seq(ScriptedPlugin.scriptedSettings: _*)

scalaSource in Compile <<= baseDirectory { (base) => base / "src" }

sbtTestDirectory <<= baseDirectory { (base) => base / "sbt-test" }

crossScalaVersions := Seq("2.9.1", "2.9.2")

scalacOptions ++= Seq("-deprecation", "-unchecked")

