import catext.Dependencies._
import microsites.MicrositeKeys.{micrositeBaseUrl, micrositeDescription, micrositeDocumentationUrl, micrositeGithubOwner, micrositeGithubRepo, micrositeName, micrositePalette}

addCommandAlias("debug", "; clean ; test")

addCommandAlias("validate", "; +clean ; +test; makeMicrosite")

onLoad in Global := (Command.process("project freestyle", _: State)) compose (onLoad in Global).value

val dev  = Seq(Dev("47 Degrees (twitter: @47deg)", "47 Degrees"))
val gh   = GitHubSettings("com.fortysevendeg", "freestyle", "47 Degrees", apache)
val vAll = Versions(versions, libraries, scalacPlugins)

lazy val commonSettings = Seq(
  scalaVersion in ThisBuild := "2.12.0",
  crossScalaVersions in ThisBuild := Seq("2.11.8", "2.12.0"),
  scalaOrganization in ThisBuild := "org.typelevel",
  organization := gh.org,
  organizationName := gh.publishOrg,
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
  scalafmtConfig in ThisBuild := Some(file(".scalafmt.conf"))
) ++
  reformatOnCompileSettings ++
  sharedCommonSettings ++
  sharedReleaseProcess ++
  credentialSettings ++
  sharedPublishSettings(gh, dev) ++
  miscSettings ++
  addCompilerPlugins(vAll, "paradise", "kind-projector")

lazy val micrositeSettings = Seq(
  micrositeName := "Freestyle",
  micrositeDescription := "A Cohesive & Pragmatic Framework of FP centric Scala libraries",
  micrositeDocumentationUrl := "/docs/",
  micrositeGithubOwner := "47deg",
  micrositeGithubRepo := "freestyle",
  micrositeHighlightTheme := "dracula",
  micrositeExternalLayoutsDirectory := (resourceDirectory in Compile).value / "microsite" / "layouts",
  micrositeExternalIncludesDirectory := (resourceDirectory in Compile).value / "microsite" / "includes",
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md" | "CNAME",
  micrositePalette := Map(
    "brand-primary"     -> "#01C2C2",
    "brand-secondary"   -> "#142236",
    "brand-tertiary"    -> "#202D40",
    "gray-dark"         -> "#383D44",
    "gray"              -> "#646D7B",
    "gray-light"        -> "#E6E7EC",
    "gray-lighter"      -> "#F4F5F9",
    "white-color"       -> "#E6E7EC"))

pgpPassphrase := Some(sys.env.getOrElse("PGP_PASSPHRASE", "").toCharArray)
pgpPublicRing := file(s"${sys.env.getOrElse("PGP_FOLDER", ".")}/pubring.gpg")
pgpSecretRing := file(s"${sys.env.getOrElse("PGP_FOLDER", ".")}/secring.gpg")

lazy val freestyle = (crossProject in file("freestyle")).
  settings(commonSettings: _*).
  settings(name := "freestyle").
  settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-free" % "0.9.0",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
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
      "io.monix" %%% "monix-eval" % "2.2.1",
      "io.monix" %%% "monix-cats" % "2.2.1"
    )
  ).
  jsSettings(sharedJsSettings: _*)

lazy val freestyleMonixJVM = freestyleMonix.jvm
lazy val freestyleMonixJS  = freestyleMonix.js

lazy val freestyleEffects = (crossProject in file("freestyle-effects")).
  dependsOn(freestyle).
  settings(commonSettings: _*).
  settings(name := "freestyle-effects").
  settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.1"      % "test"
    )
  ).
  jsSettings(sharedJsSettings: _*)

lazy val freestyleEffectsJVM = freestyleEffects.jvm
lazy val freestyleEffectsJS  = freestyleEffects.js

lazy val freestyleDoobie = (project in file("freestyle-doobie")).
  dependsOn(freestyleJVM).
  settings(commonSettings: _*).
  settings(name := "freestyle-doobie").
  settings(
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-core-cats" % "0.4.1",
      "org.tpolecat"  %% "doobie-h2-cats"   % "0.4.1" % "test",
      "org.scalatest" %% "scalatest"        % "3.0.1"          % "test"
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
      "eu.unicredit" %%% "shocon" % "0.1.7",
      "org.scalatest" %%% "scalatest" % "3.0.1" % "test"
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
      "com.fortysevendeg" %%% "fetch" % "0.5.0",
      "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
      "com.fortysevendeg" %%% "fetch-monix" % "0.5.0"
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
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.1" % "test"
  ).
  jvmSettings(
    libraryDependencies += "io.verizon.journal" %% "core" % "3.0.18"
  ).
  jsSettings(
    libraryDependencies += "biz.enef" %%% "slogging" % "0.5.2"
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
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalatest" %%% "scalatest" % "3.0.1" % "test"
    )
  )

lazy val docs = (project in file("docs")).
  dependsOn(freestyleJVM).
  dependsOn(freestyleEffectsJVM).
  settings(commonSettings: _*).
  settings(micrositeSettings: _*).
  settings(noPublishSettings: _*).
  settings(
    name := "docs",
    description := "freestyle docs"
  ).
  enablePlugins(MicrositesPlugin)
