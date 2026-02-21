val zioVersion = "2.1.24"
val zioHttpVersion = "3.8.1"
val zioJsonVersion = "0.7.44"
val zioConfigVersion = "4.0.6"

val liqpVersion = "0.9.2.3"

lazy val root = (project in file("."))
  .settings(
    name := "trmnl-home-screen-app",
    version := "0.1.0",
    scalaVersion := "3.3.7",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "nl.big-o" % "liqp" % liqpVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    fork := true,
    assembly / mainClass := Some("is.valsk.trmnlhomescreen.Main"),
    assembly / assemblyJarName := "trmnl-home-screen-app.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", _*) => MergeStrategy.concat
      case PathList("META-INF", _*)             => MergeStrategy.discard
      case "reference.conf"                     => MergeStrategy.concat
      case x                                    => MergeStrategy.first
    },
  )
