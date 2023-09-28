val scala3Version = "3.3.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "portfolzio",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.15",
      "dev.zio" %% "zio-config" % "4.0.0-RC16",
      "dev.zio" %% "zio-config-typesafe" % "4.0.0-RC16",
      "dev.zio" %% "zio-config-magnolia" % "4.0.0-RC16",
      "dev.zio" %% "zio-http" % "3.0.0-RC2",
      "dev.zio" %% "zio-process" % "0.7.2",
      "dev.zio" %% "zio-test" % "2.0.18" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.0.18" % Test,
      "dev.zio" %% "zio-test-magnolia" % "2.0.18" % Test,
      "dev.zio" %% "zio-json" % "0.6.2"
    )
  )

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
