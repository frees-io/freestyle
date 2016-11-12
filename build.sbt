import catext.Dependencies._

addCommandAlias("debug", "; clean ; test")

onLoad in Global := (Command.process("project freestyle", _: State)) compose (onLoad in Global).value

val dev  = Seq(Dev("47 Degrees (twitter: @47deg)", "47 Degrees"))
val gh   = GitHubSettings("com.fortysevendeg", "freestyle", "47 Degrees", apache)
val vAll = Versions(versions, libraries, scalacPlugins)

lazy val commonSettings = Seq(
  scalaVersion in ThisBuild := "2.11.8", 
  //scalaOrganization := "org.typelevel", disabled until supported by Ensime
  addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full),
  resolvers += Resolver.sonatypeRepo("releases"),
  organization := gh.org,
  organizationName := gh.publishOrg,
  homepage := Option(url("http://www.47deg.com")),
  organizationHomepage := Some(new URL("http://47deg.com")),
  startYear := Some(2016),
  description := "Freestyle is a library to help building libraries and applications based on Free monads.",
  scalacOptions in ThisBuild ++= Seq(
    //"-Ypartial-unification", // enable fix for SI-2712
    //"-Yliteral-types",       // enable SIP-23 implementation
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
    "-Xfatal-warnings",
    "-Xlint",
    "-Yinline-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
    //"-Xlog-implicits",
    //"-Xprint:typer"
    //"-Ymacro-debug-lite"
  )
) ++
  sharedCommonSettings ++
  sharedReleaseProcess ++
  credentialSettings ++
  sharedPublishSettings(gh, dev) ++
  miscSettings ++
  addLibs(vAll, "cats-free") ++
  addCompilerPlugins(vAll, "paradise", "kind-projector")

pgpPassphrase := Some(sys.env.getOrElse("PGP_PASSPHRASE", "").toCharArray)
pgpPublicRing := file(s"${sys.env.getOrElse("PGP_FOLDER", ".")}/pubring.gpg")
pgpSecretRing := file(s"${sys.env.getOrElse("PGP_FOLDER", ".")}/secring.gpg")

lazy val freestyle = (crossProject in file("freestyle")).
  settings(commonSettings: _*).
  settings(name := "freestyle").
  settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.11.8"
    )
  ).
  jsSettings(sharedJsSettings: _*)

lazy val freestyleJVM = freestyle.jvm
lazy val freestyleJS  = freestyle.js

lazy val freestyleMonix = (crossProject in file("freestyle-monix")).
  settings(commonSettings: _*).
  settings(name := "freestyle-monix").
  settings(
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix-eval" % versions("monix"),
      "io.monix" %%% "monix-cats" % versions("monix")
    )
  ).
  jsSettings(sharedJsSettings: _*)

lazy val freestyleMonixJVM = freestyleMonix.jvm
lazy val freestyleMonixJS  = freestyleMonix.js

lazy val tests = (project in file("tests")).
  dependsOn(freestyleJVM).
  dependsOn(freestyleMonixJVM).
  settings(commonSettings: _*).
  settings(noPublishSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.11.8",
      "org.scalatest" %%% "scalatest" % "3.0.0" % "test"
    )
  )

lazy val docs = (project in file("docs")).
  dependsOn(freestyleJVM).
  settings(commonSettings: _*).
  settings(noPublishSettings: _*).
  settings(
    micrositeExtraMdFiles := Map(file("README.md") -> "index.md")
  ).
  enablePlugins(MicrositesPlugin)
