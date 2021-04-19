import sbt.Keys.resolvers

lazy val akkaHttpVersion = "10.2.0"
lazy val akkaVersion = "2.6.8"
lazy val jodaTimeVersion = "2.9.4"
lazy val scalaLoggingVersion = "3.9.2"
lazy val logBackClassicVersion = "1.1.3"
lazy val scalaTagsVersion = "0.8.2"
lazy val specs2Version = "4.6.0"
lazy val logBackJsonVersion = "0.1.5"

lazy val drtCiriumVersion = "56"
lazy val drtLibVersion = "52"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "uk.gov.homeoffice.drt",
      scalaVersion := "2.12.8"
    )),

    compile := ((compile in Compile) dependsOn buildReactApp).value,

    version := sys.env.getOrElse("DRONE_BUILD_NUMBER", sys.env.getOrElse("BUILD_ID", "DEV")),
    name := "drt-dashboard",
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "joda-time" % "joda-time" % jodaTimeVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
      "ch.qos.logback" % "logback-classic" % logBackClassicVersion % Runtime,
      "com.lihaoyi" %% "scalatags" % scalaTagsVersion,
      "uk.gov.homeoffice" %% "drt-cirium" % drtCiriumVersion,
      "uk.gov.homeoffice" %% "drt-lib" % drtLibVersion,
      "ch.qos.logback.contrib" % "logback-json-classic" % logBackJsonVersion,
      "ch.qos.logback.contrib" % "logback-jackson" % logBackJsonVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.2",
      "uk.gov.service.notify" % "notifications-java-client" % "3.17.0-RELEASE",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,

      "org.specs2" %% "specs2-core" % specs2Version % Test
    ),

    resolvers += "Artifactory Release Realm" at "https://artifactory.digital.homeoffice.gov.uk/",
    resolvers += "Artifactory Realm release local" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release-local/",
    resolvers += "Spring Lib Release Repository" at "https://repo.spring.io/libs-release/",
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",

    dockerExposedPorts ++= Seq(8081),

  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)

lazy val yarnInstall = taskKey[Unit]("yarn install")

val cwd = System.getProperty("user.dir")
val reactAppDir: sbt.File = new File(cwd + "/react")

yarnInstall := {
  scala.sys.process.Process("yarn install", reactAppDir).!
}

lazy val buildReactApp = taskKey[Unit]("Build react app")
buildReactApp := {
  scala.sys.process.Process("yarn build", reactAppDir).!
  scala.sys.process.Process(s"rm -rf $cwd/src/main/resources/frontend").!
  scala.sys.process.Process(s"mv build $cwd/src/main/resources/frontend", new File(cwd + "/react")).!
}

fork in run := true
cancelable in Global := true
