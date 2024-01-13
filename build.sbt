import sbt.Keys.resolvers

lazy val drtLibVersion = "v711"
lazy val drtCiriumVersion = "203"
lazy val akkaHttpVersion = "10.5.3"
lazy val akkaVersion = "2.8.5"
lazy val jodaTimeVersion = "2.12.5"
lazy val scalaLoggingVersion = "3.9.5"
lazy val logBackClassicVersion = "1.4.13"
lazy val scalaTagsVersion = "0.12.0"
lazy val specs2Version = "4.20.3"
lazy val logBackJsonVersion = "0.1.5"
lazy val scalaTestVersion = "3.2.17"
lazy val janinoVersion = "3.1.11"
lazy val jacksonDatabindVersion = "2.16.1"
lazy val notificationsJavaClientVersion = "4.1.1-RELEASE"
lazy val scalaCsvVersion = "1.3.10"
lazy val slickVersion = "3.4.1"
lazy val awsJava2SdkVersion = "2.21.40"
lazy val postgresqlVersion = "42.7.0"
lazy val mockitoVersion = "4.6.1"
lazy val poiScalaVersion ="0.24"
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "uk.gov.homeoffice.drt",
      scalaVersion := "2.13.12"
    )),

    version := sys.env.getOrElse("DRONE_BUILD_NUMBER", sys.env.getOrElse("BUILD_ID", "DEV")),
    name := "drt-dashboard",
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-caching" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "joda-time" % "joda-time" % jodaTimeVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
      "ch.qos.logback" % "logback-classic" % logBackClassicVersion % Runtime,
      "com.lihaoyi" %% "scalatags" % scalaTagsVersion,
      "uk.gov.homeoffice" %% "drt-cirium" % drtCiriumVersion,
      "uk.gov.homeoffice" %% "drt-lib" % drtLibVersion excludeAll("org.scala-lang.modules", "scala-xml"),
      "ch.qos.logback.contrib" % "logback-json-classic" % logBackJsonVersion,
      "ch.qos.logback.contrib" % "logback-jackson" % logBackJsonVersion,
      "org.codehaus.janino" % "janino" % janinoVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion,
      "uk.gov.service.notify" % "notifications-java-client" % notificationsJavaClientVersion,
      "com.github.tototoshi" %% "scala-csv" % scalaCsvVersion,
      "org.scalactic" %% "scalactic" % scalaTestVersion,
      "software.amazon.awssdk" % "s3" % awsJava2SdkVersion,
      "info.folone" %% "poi-scala" % poiScalaVersion,
      "com.typesafe.slick" %% "slick" % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
      "org.postgresql" % "postgresql" % postgresqlVersion,

      "com.h2database" % "h2" % "2.2.224" % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.mockito" % "mockito-core" % mockitoVersion % Test,

),

    resolvers += "Artifactory Release Realm" at "https://artifactory.digital.homeoffice.gov.uk/",
    resolvers += "Artifactory Realm release local" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release-local/",
    resolvers += "Spring Lib Release Repository" at "https://repo.spring.io/libs-release/",
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",

    dockerExposedPorts ++= Seq(8081),

  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)

Test / parallelExecution := false

Test / javaOptions += "-Duser.timezone=UTC"

Runtime / javaOptions += "-Duser.timezone=UTC"

run / fork := true
cancelable in Global := true
