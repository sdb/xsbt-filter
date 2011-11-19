sbtPlugin := true

name := "xsbt-filter"

organization := "com.github.sdb"

version := "0.1-SNAPSHOT"

// seq(ScriptedPlugin.scriptedSettings: _*)

scalaSource in Compile <<= baseDirectory { (base) => base / "src" }

// sbtTestDirectory <<= baseDirectory { (base) => base / "sbt-test" }
