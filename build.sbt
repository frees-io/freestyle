
addCommandAlias("debug", "; clean ; test:compile")

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8",
  scalaOrganization := "org.typelevel",
  resolvers += Resolver.sonatypeRepo("releases"),
  organization := "io.freestyle",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.0" cross CrossVersion.binary),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-free" % "0.7.2"
    ),
  scalacOptions ++= Seq(
    "-Ypartial-unification", // enable fix for SI-2712
    "-Yliteral-types",       // enable SIP-23 implementation
    "-Xplugin-require:macroparadise",
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yinline-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
    //"-Xprint:typer"
    //"-Ymacro-debug-lite"
  )
)

lazy val root = (project in file(".")).
  aggregate(freestyle, tests)

lazy val freestyle = (project in file("freestyle")).
  settings(commonSettings: _*).
  settings(
    name := "freestyle",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.11.8",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test"
    )
  )

lazy val tests = (project in file("tests")).
  dependsOn(freestyle).
  settings(commonSettings: _*).
  settings( // other settings
  )

lazy val docs = (project in file("docs")).
  dependsOn(freestyle).
  settings(commonSettings: _*).
  settings(
    micrositeExtraMdFiles := Map(file("README.md") -> "index.md")).
  enablePlugins(MicrositesPlugin)
