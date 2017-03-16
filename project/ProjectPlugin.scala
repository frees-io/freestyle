import sbt._
import sbt.Keys._

import catext.Dependencies._
import catext.CatExtPlugin.autoImport._
import org.scalafmt.sbt.ScalaFmtPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {
  override def trigger = allRequirements

  val dev  = Seq(Dev("47 Degrees (twitter: @47deg)", "47 Degrees"))
  val gh   = GitHubSettings("com.fortysevendeg", "freestyle", "47 Degrees", apache)
  val vAll = Versions(versions, libraries, scalacPlugins)

  override def globalSettings = Seq(
    onLoad := (Command.process("project freestyle", _: State)) compose (onLoad in Global).value
  )

  override def buildSettings = Seq(
    crossScalaVersions := Seq("2.11.8", "2.12.1"),
    scalaVersion := crossScalaVersions.value.head,
    scalaOrganization := "org.typelevel",
    organization  := gh.org,
    organizationName  := gh.publishOrg
  )

  override def projectSettings = Seq(
    homepage := Option(url("http://www.47deg.com")),
    organizationHomepage := Some(new URL("http://47deg.com")),
    startYear := Some(2016),
    description := "A Cohesive & Pragmatic Framework of FP centric Scala libraries",
    scalacOptions in ThisBuild ++= Seq(
      "-Ypartial-unification", // enable fix for SI-2712
      "-Yliteral-types",       // enable SIP-23 implementation
      "-Xplugin-require:macroparadise",
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:experimental.macros",
      "-unchecked",
      //"-Xfatal-warnings",
      //"-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture"
        //"-Xlog-implicits"
        //"-Xprint:typer"
        //"-Ymacro-debug-lite"
    ),
    dependencyOverrides ++= Set(
      // scala-lang is still used during transitive ivy resolution (and eventually thrown out...)
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scalap" % scalaVersion.value,
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
      scalaOrganization.value % "scala-library" % scalaVersion.value,
      scalaOrganization.value % "scala-reflect" % scalaVersion.value,
      scalaOrganization.value % "scalap" % scalaVersion.value
    ),
    scalafmtConfig in ThisBuild := Some(file(".scalafmt.conf"))
  ) ++
  reformatOnCompileSettings ++
  sharedCommonSettings ++
  sharedReleaseProcess ++
  credentialSettings ++
  sharedPublishSettings(gh, dev) ++
  addCompilerPlugins(vAll, "paradise", "kind-projector")

}
