import sbtorgpolicies.model._

lazy val root = (project in file("."))
  .settings(moduleName := "root")
  .settings(name := "freestyle")
  .aggregate(freestyleModules: _*)

lazy val freestyle = (crossProject in file("freestyle"))
  .settings(name := "freestyle")
  .jsSettings(sharedJsSettings: _*)
  .settings(libraryDependencies ++= Seq(%("scala-reflect", scalaVersion.value)))
  .crossDepSettings(
    commonDeps ++ Seq(
      %("cats-free"),
      %("shapeless"),
      %("monix-eval") % "test",
      %("monix-cats") % "test",
      %("cats-laws")  % "test",
      %("discipline") % "test"
    ): _*
  )

lazy val freestyleJVM = freestyle.jvm
lazy val freestyleJS  = freestyle.js

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

lazy val docs = (project in file("docs"))
  .dependsOn(freestyleDependencies: _*)
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := "docs",
    description := "freestyle docs"
  )
  .settings(
    libraryDependencies ++= Seq(
      %%("play") % "test",
      %%("doobie-h2-cats"),
      %%("http4s-dsl"),
      %("h2") % "test"
    )
  )
  .enablePlugins(MicrositesPlugin)

lazy val monix = (crossProject in file("freestyle-monix"))
  .settings(name := "freestyle-monix")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ Seq(%("monix-eval"), %("monix-cats")): _*)

lazy val monixJVM = monix.jvm
lazy val monixJS  = monix.js

lazy val effects = (crossProject in file("freestyle-effects"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-effects")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val effectsJVM = effects.jvm
lazy val effectsJS  = effects.js

lazy val async = (crossProject in file("async/async"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-async")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val asyncJVM = async.jvm
lazy val asyncJS  = async.js

lazy val asyncMonix = (crossProject in file("async/monix"))
  .dependsOn(freestyle, async)
  .settings(name := "freestyle-async-monix")
  .crossDepSettings(
    commonDeps ++ Seq(
      %("monix-eval"),
      %("monix-cats")
    ): _*)
  .jsSettings(sharedJsSettings: _*)

lazy val asyncMonixJVM = asyncMonix.jvm
lazy val asyncMonixJS  = asyncMonix.js

lazy val asyncFs = (crossProject in file("async/fs2"))
  .dependsOn(freestyle, async)
  .settings(name := "freestyle-async-fs2")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ Seq(%("fs2-core"), %("fs2-cats")): _*)

lazy val asyncFsJVM = asyncFs.jvm
lazy val asyncFsJS  = asyncFs.js

lazy val cache = (crossProject in file("freestyle-cache"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-cache")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val cacheJVM = cache.jvm
lazy val cacheJS  = cache.js

lazy val cacheRedis = (project in file("freestyle-cache-redis"))
  .dependsOn(freestyleJVM, cacheJVM)
  .settings(
    name := "freestyle-cache-redis",
    resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/",
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      %%("rediscala"),
      %%("akka-actor")    % "test",
      %("embedded-redis") % "test"
    )
  )

lazy val doobie = (project in file("freestyle-doobie"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-doobie")
  .settings(
    libraryDependencies ++= Seq(
      %%("doobie-core-cats"),
      %%("doobie-h2-cats") % "test"
    ) ++ commonDeps
  )

lazy val slick = (project in file("freestyle-slick"))
  .dependsOn(freestyleJVM, asyncJVM)
  .settings(name := "freestyle-slick")
  .settings(
    libraryDependencies ++= Seq(
      %%("slick"),
      %("h2") % "test"
    ) ++ commonDeps
  )

lazy val twitterUtil = (project in file("freestyle-twitter-util"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-twitter-util")
  .settings(
    libraryDependencies ++= %%("catbird-util") +: commonDeps
  )

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
      %("config", "1.2.1")
    ) ++ commonDeps
  )

lazy val fetch = (crossProject in file("freestyle-fetch"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-fetch")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(
    commonDeps ++ Seq(
      %("fetch"),
      %("fetch-monix")
    ): _*
  )

lazy val fetchJVM = fetch.jvm
lazy val fetchJS  = fetch.js

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
  .crossDepSettings(commonDeps: _*)

lazy val loggingJVM = logging.jvm
lazy val loggingJS  = logging.js

lazy val fs2 = (crossProject in file("freestyle-fs2"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-fs2")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(
    commonDeps ++ Seq(
      %("fs2-core")
    ): _*
  )

lazy val fs2JVM = fs2.jvm
lazy val fs2JS  = fs2.js

lazy val httpHttp4s = (project in file("http/http4s"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-http4s")
  .settings(
    libraryDependencies ++= Seq(
      %%("http4s-core"),
      %%("http4s-dsl") % "test"
    ) ++ commonDeps
  )

lazy val httpFinch = (project in file("http/finch"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-finch")
  .settings(
    libraryDependencies ++= %%("finch-core") +: commonDeps
  )

lazy val httpAkka = (project in file("http/akka"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-akka")
  .settings(
    libraryDependencies ++= Seq(
      %%("akka-http"),
      %%("akka-http-testkit") % "test"
    ) ++ commonDeps
  )

lazy val httpPlay = (project in file("http/play"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-play")
  .settings(
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      %%("play") % "test",
      %%("play-test") % "test"
    ) ++ commonDeps
  )

lazy val todolistExample = (project in file("freestyle-examples/todolist"))
  .dependsOn(freestyleJVM)
  .dependsOn(doobie)
  .dependsOn(httpFinch)
  .dependsOn(fs2JVM)
  .settings(name := "freestyle-examples-todolist")
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      %%("circe-generic"),
      %%("doobie-h2-cats"),
      "com.github.finagle" %% "finch-circe" % "0.14.0",
      "com.twitter" %% "twitter-server" % "1.29.0"
    ) ++ commonDeps
  )

addCommandAlias("debug", "; clean ; test")

addCommandAlias("validate", "; +clean ; +test; makeMicrosite")

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

orgAfterCISuccessTaskListSetting := List(
  orgCreateFiles.toOrgTask,
  orgCommitPolicyFiles.toOrgTask,
  depUpdateDependencyIssues.toOrgTask,
  (publishMicrosite in docs).toOrgTask,
  orgPublishReleaseTask.toOrgTask(allModulesScope = true, crossScalaVersionsScope = true)
)

lazy val freestyleModules: Seq[ProjectReference] = Seq(
  freestyleJVM,
  freestyleJS,
  monixJVM,
  monixJS,
  effectsJVM,
  effectsJS,
  asyncJVM,
  asyncJS,
  asyncMonixJVM,
  asyncMonixJS,
  asyncFsJVM,
  asyncFsJS,
  cacheJVM,
  cacheJS,
  cacheRedis,
  doobie,
  slick,
  twitterUtil,
  config,
  fetchJVM,
  fetchJS,
  loggingJVM,
  loggingJS,
  fs2JVM,
  fs2JS,
  httpHttp4s,
  httpFinch,
  httpAkka,
  httpPlay,
  todolistExample
)

lazy val freestyleDependencies: Seq[ClasspathDependency] =
  freestyleModules.map(ClasspathDependency(_, None))
