import catext.Dependencies._

addCommandAlias("debug", "; clean ; test")

onLoad in Global := (Command.process("project freestyle", _: State)) compose (onLoad in Global).value

val dev  = Seq(Dev("47 Degrees (twitter: @47deg)", "47 Degrees"))
val gh   = GitHubSettings("com.fortysevendeg", "freestyle", "47 Degrees", apache)
val vAll = Versions(versions, libraries, scalacPlugins)

lazy val commonSettings = Seq(
  scalaVersion in ThisBuild := "2.11.8",
  scalaOrganization in ThisBuild := "org.typelevel",//, disabled until supported by Ensime
  //addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full),
  resolvers ++= Seq(Resolver.sonatypeRepo("snapshots"), Resolver.sonatypeRepo("releases")),
  organization := gh.org,
  organizationName := gh.publishOrg,
  homepage := Option(url("http://www.47deg.com")),
  organizationHomepage := Some(new URL("http://47deg.com")),
  startYear := Some(2016),
  description := "Freestyle is a library to help building libraries and applications based on Free monads.",
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
    "-Yinline-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
    //"-Xlog-implicits"
    //"-Xprint:typer"
    //"-Ymacro-debug-lite"
  )
) ++
  sharedCommonSettings ++
  sharedReleaseProcess ++
  credentialSettings ++
  sharedPublishSettings(gh, dev) ++
  miscSettings ++
  addCompilerPlugins(vAll, "paradise", "kind-projector")

pgpPassphrase := Some(sys.env.getOrElse("PGP_PASSPHRASE", "").toCharArray)
pgpPublicRing := file(s"${sys.env.getOrElse("PGP_FOLDER", ".")}/pubring.gpg")
pgpSecretRing := file(s"${sys.env.getOrElse("PGP_FOLDER", ".")}/secring.gpg")

lazy val freestyle = (crossProject in file("freestyle")).
  settings(commonSettings: _*).
  settings(name := "freestyle").
  settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-free" % "0.8.1",
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
      "io.monix" %%% "monix-eval" % "2.1.0",
      "io.monix" %%% "monix-cats" % "2.1.0"
    )
  ).
  jsSettings(sharedJsSettings: _*)

lazy val freestyleMonixJVM = freestyleMonix.jvm
lazy val freestyleMonixJS  = freestyleMonix.js

lazy val freestyleDoobie = (project in file("freestyle-doobie")).
  dependsOn(freestyleJVM).
  settings(commonSettings: _*).
  settings(name := "freestyle-doobie").
  settings(
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-core-cats" % "0.3.1-SNAPSHOT",
      "org.tpolecat"  %% "doobie-h2-cats"   % "0.3.1-SNAPSHOT" % "test",
      "org.scalatest" %% "scalatest"        % "3.0.0"          % "test"
    )
  )

lazy val fixResources = taskKey[Unit](
    "Fix application.conf presence on first clean build.")

lazy val freestyleConfig = (crossProject in file("freestyle-config")).
  dependsOn(freestyle).
  settings(commonSettings: _*).
  settings(
    name := "freestyle-config",
    fixResources := {
      val testConf = (resourceDirectory in Test).value / "application.conf"
      if (testConf.exists) {
        IO.copyFile(
          testConf,
          (classDirectory in Compile).value / "application.conf"
        )
      }
    },
    compile in Test := ((compile in Test) dependsOn fixResources).value
  ).
  settings(
    libraryDependencies ++= Seq(
      "eu.unicredit" %%% "shocon" % "0.1.4",
      "org.scalatest" %%% "scalatest" % "3.0.0" % "test"
    )
  )

lazy val freestyleConfigJVM = freestyleConfig.jvm
lazy val freestyleConfigJS  = freestyleConfig.js


lazy val freestyleFetch = (crossProject in file("freestyle-fetch")).
  dependsOn(freestyle).
  settings(commonSettings: _*).
  settings(name := "freestyle-fetch").
  settings(
    libraryDependencies ++= Seq(
      "com.fortysevendeg" %%% "fetch" % "0.4.0",
      "org.scalatest" %%% "scalatest" % "3.0.0" % "test",
      "com.fortysevendeg" %%% "fetch-monix" % "0.4.0"
     )
  ).
  jsSettings(sharedJsSettings: _*)

lazy val freestyleFetchJVM = freestyleFetch.jvm
lazy val freestyleFetchJS  = freestyleFetch.js

lazy val freestyleLogging = (crossProject in file("freestyle-logging")).
  dependsOn(freestyle).
  settings(commonSettings: _*).
  settings(name := "freestyle-logging").
  settings(
    libraryDependencies ++= Seq(
      "io.verizon.journal" %% "core" % "2.3.16",
      "org.scalatest" %% "scalatest" % "3.0.0" % "test"
    )
  ).
  jsSettings(sharedJsSettings: _*)

lazy val freestyleLoggingJVM = freestyleLogging.jvm
lazy val freestyleLoggingJS  = freestyleLogging.js

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
