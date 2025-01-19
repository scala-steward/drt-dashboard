ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.12")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.2.9")
addSbtPlugin("net.vonbuchholtz" %% "sbt-dependency-check" % "5.1.0")
addDependencyTreePlugin
