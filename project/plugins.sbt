libraryDependencies <+= sbtVersion(v => v.split('.') match {
    case Array("0", "12", _) => "org.scala-sbt" % "scripted-plugin" % v
    case _                   => "org.scala-sbt" %% "scripted-plugin" % v
})
