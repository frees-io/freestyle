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
    wartremoverWarnings in (Test, compile) ++= Seq(Wart.FinalCaseClass, Wart.ExplicitImplicitTypes),
    wartremoverWarnings in (Test, compile) -= Wart.NonUnitStatements
  )
  .crossDepSettings(
    commonDeps ++ Seq(
      %("cats-free"),
      %("iota-core"),
      %("simulacrum"),
      %("shapeless") % "test",
      %("cats-laws") % "test",
      %%("mainecoon-core")
    ): _*
  )

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js

lazy val tests = jvmModule("tests")
  .dependsOn(coreJVM % "compile->compile;test->test")
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= commonDeps ++ Seq(
      %("scala-reflect", scalaVersion.value),
      %%("pcplod")     % "test",
      %%("monix-eval") % "test"
    ),
    fork in Test := true,
    javaOptions in Test ++= {
      val excludedScalacOptions: List[String] = List("-Yliteral-types")
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
        "freestyle.free.bench.BenchBoiler",
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
  .crossDepSettings(commonDeps ++ Seq(%("cats-mtl-core")): _*)

lazy val effectsJVM = effects.jvm
lazy val effectsJS  = effects.js

lazy val async = module("async", subFolder = Some("async"))
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ Seq(%("cats-effect") % Test): _*)

lazy val asyncJVM = async.jvm
lazy val asyncJS  = async.js

lazy val asyncCatsEffect = module("async-cats-effect", subFolder = Some("async"))
  .dependsOn(core, async)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ Seq(%("cats-effect")): _*)

lazy val asyncCatsEffectJVM = asyncCatsEffect.jvm
lazy val asyncCatsEffectJS  = asyncCatsEffect.js

lazy val asyncGuava = jvmModule("async-guava", subFolder = Some("async"))
  .dependsOn(coreJVM, asyncJVM)
  .settings(libraryDependencies ++= commonDeps ++ Seq(%("guava")))

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
      %("config"),
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
  .crossDepSettings(commonDeps ++ Seq(%%("sourcecode")): _*)

lazy val loggingJVM = logging.jvm
lazy val loggingJS  = logging.js

//////////////////////
//// INTEGRATIONS ////
//////////////////////

lazy val monix = module("monix", full = false, subFolder = Some("integrations"))
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++
    Seq(%("monix-eval")): _*)

lazy val monixJVM = monix.jvm
lazy val monixJS  = monix.js

lazy val cacheRedis = jvmModule("cache-redis", subFolder = Some("integrations"))
  .dependsOn(coreJVM, cacheJVM)
  .settings(
    resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/",
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      %%("rediscala"),
      %%("akka-actor")    % "test",
      %("embedded-redis") % "test"
    ) ++ commonDeps
  )

lazy val doobie = jvmModule("doobie", subFolder = Some("integrations"))
  .dependsOn(coreJVM)
  .settings(
    libraryDependencies ++= Seq(
      %%("doobie-core"),
      %%("doobie-h2") % "test"
    ) ++ commonDeps
  )

lazy val slick = jvmModule("slick", subFolder = Some("integrations"))
  .dependsOn(coreJVM, asyncJVM)
  .settings(
    libraryDependencies ++= Seq(
      %%("slick"),
      %("h2") % "test"
    ) ++ commonDeps
  )

lazy val twitterUtil = jvmModule("twitter-util", subFolder = Some("integrations"))
  .dependsOn(coreJVM)
  .settings(
    libraryDependencies ++= Seq(%%("catbird-util")) ++ commonDeps
  )

lazy val fetch = module("fetch", subFolder = Some("integrations"))
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(
    commonDeps ++ Seq(%("fetch")): _*
  )

lazy val fetchJVM = fetch.jvm
lazy val fetchJS  = fetch.js

/////////////////////////////
//// INTEGRATIONS - HTTP ////
/////////////////////////////

lazy val httpHttp4s = jvmModule("http4s", subFolder = Some("integrations/http"))
  .dependsOn(coreJVM)
  .settings(
    libraryDependencies ++= Seq(
      %%("http4s-core"),
      %%("http4s-dsl") % "test"
    ) ++ commonDeps
  )

lazy val httpFinch = jvmModule("finch", subFolder = Some("integrations/http"))
  .dependsOn(coreJVM)
  .settings(
    libraryDependencies ++= Seq(%%("finch-core")) ++ commonDeps
  )

lazy val httpAkka = jvmModule("akka", subFolder = Some("integrations/http"))
  .dependsOn(coreJVM)
  .settings(
    libraryDependencies ++= Seq(
      %%("akka-http"),
      %%("akka-http-testkit") % "test"
    ) ++ commonDeps
  )

lazy val httpPlay = jvmModule("play", subFolder = Some("integrations/http"))
  .dependsOn(coreJVM)
  .settings(
    concurrentRestrictions in Global := Seq(Tags.limitAll(1)),
    libraryDependencies ++= Seq(
      %%("play")      % "test",
      %%("play-test") % "test"
    ) ++ commonDeps
  )

lazy val httpClient = module("http-client", subFolder = Some("integrations/http"))
  .dependsOn(core)
  .settings(resolvers += Resolver.jcenterRepo)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(
    commonDeps ++ Seq(%("hammock-core"), %("cats-effect") % "test"): _*
  )

lazy val httpClientJS  = httpClient.js
lazy val httpClientJVM = httpClient.jvm

//////////////////
//// EXAMPLES ////
//////////////////

lazy val todolist = jvmModule("todolist", subFolder = Some("examples"))
  .dependsOn(coreJVM, doobie, httpFinch, loggingJVM, effectsJVM, config)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      %%("cats-effect"),
      %%("circe-generic"),
      %%("doobie-h2"),
      %%("doobie-hikari"),
      %%("finch-circe"),
      %%("twitter-server")
    ) ++ commonDeps
  )

lazy val slickExample = jvmModule("slick-example", subFolder = Some("examples"))
  .dependsOn(coreJVM, loggingJVM, slick)
  .settings(noPublishSettings: _*)
  .settings(slickGen := slickCodeGenTask.value) // register manual sbt command
  .settings(
    libraryDependencies ++= Seq(
      "org.postgresql"     % "postgresql"     % "42.1.1",
      "com.typesafe.slick" %% "slick-codegen" % "3.2.0"
    ) ++ commonDeps
  )

/////////////////////
//// ALL MODULES ////
/////////////////////

lazy val jvmModules: Seq[ProjectReference] = Seq(
  coreJVM,
  effectsJVM,
  asyncJVM,
  asyncCatsEffectJVM,
  asyncGuava,
  cacheJVM,
  config,
  loggingJVM,
  //Integrations:
  monixJVM,
  cacheRedis,
  doobie,
  slick,
  twitterUtil,
  fetchJVM,
  httpHttp4s,
  httpFinch,
  httpAkka,
  httpPlay,
  httpClientJVM,
  //tests,
  //Examples:
  todolist,
  slickExample
)

lazy val jsModules: Seq[ProjectReference] = Seq(
  coreJS,
  effectsJS,
  asyncJS,
  asyncCatsEffectJS,
  cacheJS,
  loggingJS,
  //Integrations:
  monixJS,
  fetchJS,
  httpClientJS
)

lazy val allModules: Seq[ProjectReference] = jvmModules ++ jsModules

lazy val jvmFreestyleDeps: Seq[ClasspathDependency] =
  jvmModules.map(ClasspathDependency(_, None))

addCommandAlias("validateDocs", ";project docs;tut;project root")
addCommandAlias(
  "validateJVM",
  (List("fixResources") ++ toCompileTestList(jvmModules) ++ List("project root")).asCmd)
addCommandAlias("validateJS", (toCompileTestList(jsModules) ++ List("project root")).asCmd)
addCommandAlias(
  "validate",
  ";clean;compile;coverage;validateJVM;coverageReport;coverageAggregate;coverageOff")

///////////////
//// DOCS ////
///////////////

lazy val docs = (project in file("docs"))
  .dependsOn(jvmFreestyleDeps: _*)
  .settings(moduleName := "frees-docs")
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    addCompilerPlugin(%%("scalameta-paradise") cross CrossVersion.full),
    libraryDependencies += %%("scalameta", "1.8.0"),
    scalacOptions += "-Xplugin-require:macroparadise"
  )
  .settings(
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.bintrayRepo("kailuowang", "maven"),
      Resolver.bintrayRepo("tabdulradi", "maven")
    ),
    libraryDependencies ++= Seq(
      %%("monix"),
      %%("doobie-h2"),
      %%("http4s-dsl"),
      %%("play"),
      %("h2") % "test"
    )
  )
  .settings(
    scalacOptions in Tut ~= (_ filterNot Set("-Ywarn-unused-import", "-Xlint").contains)
  )
  .settings(
    micrositeGithubToken := getEnvVar(orgGithubTokenSetting.value)
  )
  .enablePlugins(MicrositesPlugin)
  .disablePlugins(ProjectPlugin)

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")
