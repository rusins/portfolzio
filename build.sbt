val scala3Version = "3.3.0"

lazy val ZioVersion = "2.0.18"

lazy val root = project
  .in(file("."))
  .settings(
    name := "portfolzio",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % ZioVersion,
      "dev.zio" %% "zio-config" % "4.0.0-RC16",
      "dev.zio" %% "zio-config-typesafe" % "4.0.0-RC16",
      "dev.zio" %% "zio-config-magnolia" % "4.0.0-RC16",
      "dev.zio" %% "zio-http" % "3.0.0-RC2",
      "dev.zio" %% "zio-process" % "0.7.2",
      "dev.zio" %% "zio-test" % ZioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % ZioVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % ZioVersion % Test,
      "dev.zio" %% "zio-json" % "0.6.2",
      "dev.zio" %% "zio-logging" % "2.1.14",
    ),
  )

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
