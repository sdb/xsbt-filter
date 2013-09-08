libraryDependencies <+= sbtVersion(v => v.split('.') match {
    case Array("0", "13", _) => "org.scala-sbt" % "scripted-plugin" % v
    case Array("0", "12", _) => "org.scala-sbt" % "scripted-plugin" % v
    case _                   => "org.scala-sbt" %% "scripted-plugin" % v
})

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")
