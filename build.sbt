val scala3Version = "3.3.0"

lazy val ZioVersion = "2.0.18"

lazy val root = project
  .in(file("."))
  .settings(
    name := "portfolzio",
    version := "1.0.0",
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
      "org.apache.commons" % "commons-imaging" % "1.0-alpha3",
    ),
  )

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

scalacOptions ++= Seq(
  "-encoding",
  "utf8",
  "-feature",
  "-unchecked",
  "-Werror",
  "-deprecation",
  "-language:implicitConversions",
)

// https://stackoverflow.com/a/48173709
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.first
}
