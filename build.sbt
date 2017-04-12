import sbtorgpolicies.model._
import sbtorgpolicies.libraries.v

lazy val root = project.in(file("."))
  .settings(moduleName := "root")
  .settings(name := "freestyle")
  .settings(noPublishSettings)
  .aggregate(rootJVM, rootJS)
  .dependsOn(freestyleJvmDependencies: _*)
  .dependsOn(freestyleJsDependencies: _*)
  .dependsOn(tests % "test-internal -> test")

lazy val rootJVM = project.in(file(".rootJVM"))
  .settings(moduleName := "rootJVM")
  .aggregate(freestyleJvmModules: _*)
  .aggregate(docs, tests)
  .dependsOn(freestyleJvmDependencies: _*)
  .dependsOn(tests % "test-internal -> test")

lazy val rootJS = project.in(file(".rootJS"))
  .settings(moduleName := "rootJS")
  .aggregate(freestyleJsModules: _*)
  .dependsOn(freestyleJsDependencies: _*)
  .enablePlugins(ScalaJSPlugin)

lazy val sharedTestSettings = Seq(
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.1" % "test"
)

lazy val tests = (project in file("tests"))
  .dependsOn(freestyleJVM)
  .settings(noPublishSettings)
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      scalaOrganization.value  % "scala-reflect" % scalaVersion.value,
      "org.ensime"            %% "pcplod"        % v("pcplod")
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
  .settings(noPublishSettings)

lazy val docs = (project in file("docs"))
  .dependsOn(freestyleJVM, effectsJVM, fs2JVM, fetchJVM, cacheJVM, loggingJVM, asyncFs2JVM, asyncMonixJVM)
  .dependsOn(doobie, slick, httpPlay, httpHttp4s, httpFinch, config)
  .settings(micrositeSettings)
  .settings(noPublishSettings)
  .settings(
    name := "docs",
    description := "freestyle docs"
  )
  .settings(
    libraryDependencies += "org.http4s" %% "http4s-dsl" % v("http4s")
  )
  .enablePlugins(MicrositesPlugin)

lazy val freestyle = (crossProject in file("freestyle"))
  .settings(name := "freestyle")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      scalaOrganization.value   % "scala-reflect" % scalaVersion.value,
      "org.typelevel"         %%% "cats-free"     % v("cats"),
      "com.chuusai"           %%% "shapeless"     % v("shapeless"),
      "io.monix"              %%% "monix-eval"    % v("monix")      % "test",
      "io.monix"              %%% "monix-cats"    % v("monix")      % "test",
      "org.typelevel"         %%% "cats-laws"     % v("cats")       % "test",
      "org.typelevel"         %%% "discipline"    % v("discipline") % "test"
    )
  )
  .jsSettings(sharedJsSettings)

lazy val freestyleJVM = freestyle.jvm
lazy val freestyleJS  = freestyle.js

lazy val monix = (crossProject in file("freestyle-monix"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-monix")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix-eval" % v("monix"),
      "io.monix" %%% "monix-cats" % v("monix")
    )
  )
  .jsSettings(sharedJsSettings)

lazy val monixJVM = monix.jvm
lazy val monixJS  = monix.js

lazy val effects = (crossProject in file("freestyle-effects"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-effects")
  .settings(sharedTestSettings)
  .jsSettings(sharedJsSettings)

lazy val effectsJVM = effects.jvm
lazy val effectsJS  = effects.js

lazy val async = (crossProject in file("async/async"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-async")
  .settings(sharedTestSettings)
  .jsSettings(sharedJsSettings)

lazy val asyncJVM = async.jvm
lazy val asyncJS  = async.js

lazy val asyncMonix = (crossProject in file("async/monix"))
  .dependsOn(freestyle, async)
  .settings(name := "freestyle-async-monix")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix-eval" % v("monix"),
      "io.monix" %%% "monix-cats" % v("monix")
    )
  )
  .jsSettings(sharedJsSettings)

lazy val asyncMonixJVM = asyncMonix.jvm
lazy val asyncMonixJS  = asyncMonix.js

lazy val asyncFs2 = (crossProject in file("async/fs2"))
  .dependsOn(freestyle, async)
  .settings(name := "freestyle-async-fs2")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-core" % v("fs2"),
      "co.fs2" %%% "fs2-cats" % v("fs2-cats")
    )
  )
  .jsSettings(sharedJsSettings)

lazy val asyncFs2JVM = asyncFs2.jvm
lazy val asyncFs2JS  = asyncFs2.js

lazy val cache = (crossProject in file("freestyle-cache"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-cache")
  .settings(sharedTestSettings)
  .jsSettings(sharedJsSettings)

lazy val cacheJVM = cache.jvm
lazy val cacheJS  = cache.js

lazy val cacheRedis = (project in file("freestyle-cache-redis"))
  .dependsOn(freestyleJVM, cacheJVM)
  .settings(name := "freestyle-cache-redis")
  .settings(sharedTestSettings)
  .settings(
    resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/",
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      "com.github.etaty"          %% "rediscala"      % v("rediscala"),
      "com.typesafe.akka"         %% "akka-actor"     % v("akka"),
      "com.orange.redis-embedded"  % "embedded-redis" % v("embedded-redis")
    )
  )

lazy val doobie = (project in file("freestyle-doobie"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-doobie")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core-cats" % v("doobie"),
      "org.tpolecat" %% "doobie-h2-cats"   % v("doobie")
    )
  )

lazy val slick = (project in file("freestyle-slick"))
  .dependsOn(freestyleJVM, asyncJVM)
  .settings(name := "freestyle-slick")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % v("slick"),
      "com.h2database"      % "h2"    % "1.4.194" % "test"
    )
  )

lazy val twitterUtil = (project in file("freestyle-twitter-util"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-twitter-util")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies += "io.catbird" %% "catbird-util" %v("catbird")
  )

lazy val config = (project in file("freestyle-config"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-config")
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
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies += "com.typesafe" % "config" % v("config")
  )

lazy val fetch = (crossProject in file("freestyle-fetch"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-fetch")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.47deg" %%% "fetch"       % v("fetch"),
      "com.47deg" %%% "fetch-monix" % v("fetch")
    )
  )
  .jsSettings(sharedJsSettings)

lazy val fetchJVM = fetch.jvm
lazy val fetchJS  = fetch.js

lazy val logging = (crossProject in file("freestyle-logging"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-logging")
  .settings(sharedTestSettings)
  .jvmSettings(
    libraryDependencies += "io.verizon.journal" %% "core" % v("journal")
  )
  .jsSettings(
    libraryDependencies += "biz.enef" %%% "slogging" % v("slogging")
  )
  .jsSettings(sharedJsSettings)

lazy val loggingJVM = logging.jvm
lazy val loggingJS  = logging.js

lazy val fs2 = (crossProject in file("freestyle-fs2"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-fs2")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies += "co.fs2" %%% "fs2-core" % v("fs2")
  )
  .jsSettings(sharedJsSettings)

lazy val fs2JVM = fs2.jvm
lazy val fs2JS  = fs2.js

lazy val httpHttp4s = (project in file("http/http4s"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-http4s")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % v("http4s"),
      "org.http4s" %% "http4s-dsl"  % v("http4s") % "test"
    )
  )

lazy val httpFinch = (project in file("http/finch"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-finch")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies += "com.github.finagle" %% "finch-core" % v("finch")
  )

lazy val httpAkka = (project in file("http/akka"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-akka")
  .settings(sharedTestSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"         % v("akka-http"),
      "com.typesafe.akka" %% "akka-http-testkit" % v("akka-http") % "test"
    )
  )

lazy val httpPlay = (project in file("http/play"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-play")
  .settings(sharedTestSettings)
  .settings(
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play"      % v("play"),
      "com.typesafe.play" %% "play-test" % v("play") % "test"
    )
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

lazy val freestyleJvmModules: Seq[ProjectReference] = Seq(
  freestyleJVM,
  monixJVM,
  effectsJVM,
  asyncJVM,
  asyncMonixJVM,
  asyncFs2JVM,
  cacheJVM,
  cacheRedis,
  doobie,
  slick,
  twitterUtil,
  config,
  fetchJVM,
  loggingJVM,
  fs2JVM,
  httpHttp4s,
  httpFinch,
  httpAkka,
  httpPlay
)

lazy val freestyleJsModules: Seq[ProjectReference] = Seq(
  freestyleJVM,
  freestyleJS,
  monixJS,
  effectsJS,
  asyncJS,
  asyncMonixJS,
  asyncFs2JS,
  cacheJS,
  fetchJS,
  loggingJS,
  fs2JS
)

lazy val freestyleModules: Seq[ProjectReference] =
  freestyleJvmModules ++ freestyleJsModules

lazy val freestyleJvmDependencies: Seq[ClasspathDependency] =
  freestyleJvmModules.map(ClasspathDependency(_, None))

lazy val freestyleJsDependencies: Seq[ClasspathDependency] =
  freestyleJsModules.map(ClasspathDependency(_, None))
