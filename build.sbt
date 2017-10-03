import sbtorgpolicies.runnable.syntax._

lazy val root = (project in file("."))
  .settings(moduleName := "root")
  .settings(name := "freestyle")
  .settings(noPublishSettings: _*)
  .aggregate(allModules: _*)

lazy val core = module("core")
  .jsSettings(sharedJsSettings: _*)
  .settings(libraryDependencies ++= Seq(%("scala-reflect", scalaVersion.value)))
  .settings(
    wartremoverWarnings in (Test, compile) := Warts.unsafe,
    wartremoverWarnings in (Test, compile) ++= Seq(
      Wart.FinalCaseClass,
      Wart.ExplicitImplicitTypes),
    wartremoverWarnings in (Test, compile) -= Wart.NonUnitStatements
  )
  .crossDepSettings(
    commonDeps ++ Seq(
      %("iota-core"),
      %("cats-free"),
      %("shapeless") % "test",
      %("cats-laws")  % "test",
      %("discipline") % "test"
    ): _*
  )

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js

lazy val tagless = module("tagless")
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)
  .settings(
    libraryDependencies += "com.kailuowang" %%% "mainecoon-core" % "0.1.1"
  )

lazy val taglessJVM = tagless.jvm
lazy val taglessJS  = tagless.js

lazy val tests = jvmModule("tests")
  .dependsOn(coreJVM % "compile->compile;test->test")
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= commonDeps ++ Seq(
      %("scala-reflect", scalaVersion.value),
      %%("pcplod") % "test",
      %%("monix-eval") % "test",
      %%("monix-cats") % "test"
    ),
    fork in Test := true,
    javaOptions in Test ++= {
      val excludedScalacOptions: List[String] = List("-Yliteral-types", "-Ypartial-unification")
      val options = (scalacOptions in Test).value.distinct
        .filterNot(excludedScalacOptions.contains)
        .mkString(",")
      val cp = (fullClasspath in Test).value.map(_.data).filter(_.exists()).distinct.mkString(",")
      Seq(
        s"""-Dpcplod.settings=$options""",
        s"""-Dpcplod.classpath=$cp"""
      )
    }
  )

lazy val bench = jvmModule("bench")
  .dependsOn(jvmFreestyleDeps: _*)
  .settings(
    name := "bench",
    description := "freestyle benchmark"
  )
  .enablePlugins(JmhPlugin)
  .configs(Codegen)
  .settings(inConfig(Codegen)(Defaults.configSettings))
  .settings(classpathConfiguration in Codegen := Compile)
  .settings(noPublishSettings)
  .settings(libraryDependencies ++= Seq(%%("cats-free"), %%("scalacheck")))
  .settings(inConfig(Compile)(
    sourceGenerators += Def.task {
      val path = (sourceManaged in (Compile, compile)).value / "bench.scala"
      (runner in (Codegen, run)).value.run(
        "freestyle.bench.BenchBoiler",
        Attributed.data((fullClasspath in Codegen).value),
        path.toString :: Nil,
        streams.value.log)
      path :: Nil
    }
  ))

lazy val Codegen = sbt.config("codegen").hide

lazy val effects = module("effects")
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val effectsJVM = effects.jvm
lazy val effectsJS  = effects.js

lazy val async = (crossProject in file("modules/async/async"))
  .settings(name := "frees-async")
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val asyncJVM = async.jvm
lazy val asyncJS  = async.js

lazy val asyncMonix = (crossProject in file("modules/async/monix"))
  .dependsOn(core, async)
  .settings(name := "frees-async-monix")
  .crossDepSettings(
    commonDeps ++ Seq(
      %("monix-eval"),
      %("monix-cats")
    ): _*)
  .jsSettings(sharedJsSettings: _*)

lazy val asyncMonixJVM = asyncMonix.jvm
lazy val asyncMonixJS  = asyncMonix.js

lazy val asyncFs = (crossProject in file("modules/async/fs2"))
  .dependsOn(core, async)
  .settings(name := "frees-async-fs2")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ Seq(%("fs2-core"), %("fs2-cats")): _*)

lazy val asyncFsJVM = asyncFs.jvm
lazy val asyncFsJS  = asyncFs.js

lazy val asyncGuava = (project in file("modules/async/guava"))
  .dependsOn(coreJVM, asyncJVM)
  .settings(name := "frees-async-guava")
  .settings(libraryDependencies ++= commonDeps ++ Seq(
    "com.google.guava" % "guava" % "22.0"
  ))

lazy val cache = module("cache")
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val cacheJVM = cache.jvm
lazy val cacheJS  = cache.js

lazy val config = jvmModule("config")
  .dependsOn(coreJVM)
  .settings(
    fixResources := {
      val testConf   = (resourceDirectory in Test).value / "application.conf"
      val targetFile = (classDirectory in (coreJVM, Compile)).value / "application.conf"
      if (testConf.exists) {
        IO.copyFile(
          testConf,
          targetFile
        )
      }
    },
    compile in Test := ((compile in Test) dependsOn fixResources).value
  )
  .settings(
    libraryDependencies ++= Seq(
      %("config", "1.2.1"),
      %%("classy-config-typesafe"),
      %%("classy-core")
    ) ++ commonDeps
  )

lazy val logging = module("logging")
  .dependsOn(core)
  .jvmSettings(
    libraryDependencies += %%("journal-core")
  )
  .jsSettings(
    libraryDependencies += %%%("slogging")
  )
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ Seq("com.lihaoyi" %% "sourcecode" % "0.1.3"): _*)

lazy val loggingJVM = logging.jvm
lazy val loggingJS  = logging.js

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val jvmModules: Seq[ProjectReference] = Seq(
  coreJVM,
  taglessJVM,
  effectsJVM,
  asyncJVM,
  asyncMonixJVM,
  asyncFsJVM,
  cacheJVM,
  config,
  loggingJVM
  // ,tests
)

lazy val jsModules: Seq[ProjectReference] = Seq(
  coreJS,
  taglessJS,
  effectsJS,
  asyncJS,
  asyncMonixJS,
  asyncFsJS,
  cacheJS,
  loggingJS
)

lazy val allModules: Seq[ProjectReference] = jvmModules ++ jsModules

lazy val jvmFreestyleDeps: Seq[ClasspathDependency] =
  jvmModules.map(ClasspathDependency(_, None))

addCommandAlias("validateJVM", (toCompileTestList(jvmModules) ++ List("project root")).asCmd)
addCommandAlias("validateJS", (toCompileTestList(jsModules) ++ List("project root")).asCmd)
addCommandAlias(
  "validate",
  ";clean;compile;coverage;validateJVM;coverageReport;coverageAggregate;coverageOff")
