import sbtorgpolicies.runnable.syntax._

lazy val root = (project in file("."))
  .settings(moduleName := "root")
  .settings(name := "freestyle")
  .settings(noPublishSettings: _*)
  .aggregate(allModules: _*)

lazy val freestyle = (crossProject in file("freestyle"))
  .settings(name := "freestyle")
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
      %("cats-free", "1.0.0-MF"),
      %("iota-core"),
      %("shapeless", "2.3.2"),
      %("simulacrum", "0.11.0"),
      %("cats-laws", "1.0.0-MF") % "test",
      %("monix-eval", "3.0.0-SNAPSHOT") % "test"
    ): _*
  )

lazy val freestyleJVM = freestyle.jvm
lazy val freestyleJS  = freestyle.js

lazy val tagless = (crossProject in file("freestyle-tagless"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-tagless")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)
  .settings(
    libraryDependencies += "com.kailuowang" %%% "mainecoon-core" % "0.4.0"
  )

lazy val taglessJVM = tagless.jvm
lazy val taglessJS  = tagless.js

lazy val tests = (project in file("tests"))
  .dependsOn(freestyleJVM)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= commonDeps ++ Seq(
      %("scala-reflect", scalaVersion.value),
      %%("pcplod") % "test"
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

lazy val bench = (project in file("bench"))
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

lazy val effects = (crossProject in file("freestyle-effects"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-effects")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)
  .settings(libraryDependencies += "org.typelevel" %%% "cats-mtl-core" % "0.0.2")

lazy val effectsJVM = effects.jvm
lazy val effectsJS  = effects.js

lazy val async = (crossProject in file("freestyle-async/async"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-async")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val asyncJVM = async.jvm
lazy val asyncJS  = async.js

lazy val asyncCatsEffect = (crossProject in file("freestyle-async/cats-effect"))
  .dependsOn(freestyle, async)
  .settings(name := "freestyle-async-cats-effect")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)
  .settings(libraryDependencies += "org.typelevel" %%% "cats-effect" % "0.4")

lazy val asyncCatsEffectJVM = asyncCatsEffect.jvm
lazy val asyncCatsEffectJS  = asyncCatsEffect.js

lazy val asyncGuava = (project in file("freestyle-async/guava"))
  .dependsOn(freestyleJVM, asyncJVM)
  .settings(name := "freestyle-async-guava")
  .settings(libraryDependencies ++= commonDeps ++ Seq(
    "com.google.guava" % "guava" % "22.0"
  ))

lazy val cache = (crossProject in file("freestyle-cache"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-cache")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val cacheJVM = cache.jvm
lazy val cacheJS  = cache.js

lazy val config = (project in file("freestyle-config"))
  .dependsOn(freestyleJVM)
  .settings(
    name := "freestyle-config",
    fixResources := {
      val testConf   = (resourceDirectory in Test).value / "application.conf"
      val targetFile = (classDirectory in (freestyleJVM, Compile)).value / "application.conf"
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

lazy val logging = (crossProject in file("freestyle-logging"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-logging")
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
  freestyleJVM,
  taglessJVM,
  effectsJVM,
  asyncJVM,
  asyncCatsEffectJVM,
  asyncGuava,
  cacheJVM,
  config,
  loggingJVM
)

lazy val jsModules: Seq[ProjectReference] = Seq(
  freestyleJS,
  taglessJS,
  effectsJS,
  asyncJS,
  asyncCatsEffectJS,
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
