import sbtorgpolicies.model._
import sbtorgpolicies.libraries.v

addCommandAlias("debug", "; clean ; test")

addCommandAlias("validate", "; +clean ; +test; makeMicrosite")

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project.in(file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings)
  .aggregate(freestyleJVM, freestyleJS)
  .dependsOn(freestyleJVM, freestyleJS, tests % "test-internal -> test")

lazy val freestyleJVM = project.in(file(".freestyleJVM"))
  .settings(moduleName := "freestyle")
  .aggregate(
    coreJVM, monixJVM, effectsJVM, asyncJVM, asyncMonixJVM, asyncFs2JVM, cacheJVM, cacheRedis,
    doobie, slick, twitterUtil, fetchJVM, config, loggingJVM, fs2JVM, docs, tests, 
    httpAkka, httpFinch, httpHttp4s, httpPlay
  )
  .dependsOn(
    coreJVM, monixJVM, effectsJVM, asyncJVM, asyncMonixJVM, asyncFs2JVM, cacheJVM, cacheRedis,
    doobie, slick, twitterUtil, fetchJVM, config, loggingJVM, fs2JVM, tests % "test-internal -> test",
    httpAkka, httpFinch, httpHttp4s, httpPlay
  )

lazy val freestyleJS = project.in(file(".freestyleJS"))
  .settings(moduleName := "freestyle")
  .aggregate(
    coreJS, monixJS, effectsJS, asyncJS, asyncMonixJS, asyncFs2JS,
    cacheJS, fetchJS, loggingJS, fs2JS
  )
  .dependsOn(
    coreJS, monixJS, effectsJS, asyncJS, asyncMonixJS, asyncFs2JS,
    cacheJS, fetchJS, loggingJS, fs2JS
  )
  .enablePlugins(ScalaJSPlugin)


lazy val core = (crossProject in file("freestyle-core"))
  .settings(name := "freestyle-core")
  .settings(
    libraryDependencies ++= Seq(
      scalaOrganization.value   % "scala-reflect" % scalaVersion.value,
      "org.typelevel"         %%% "cats-free"     % v("cats"),
      "com.chuusai"           %%% "shapeless"     % v("shapeless"),
      "io.monix"              %%% "monix-eval"    % v("monix") % "test",
      "io.monix"              %%% "monix-cats"    % v("monix") % "test"
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js

lazy val monix = (crossProject in file("freestyle-monix"))
  .dependsOn(core)
  .settings(name := "freestyle-monix")
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix-eval" % v("monix"),
      "io.monix" %%% "monix-cats" % v("monix")
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val monixJVM = monix.jvm
lazy val monixJS  = monix.js

lazy val effects = (crossProject in file("freestyle-effects"))
  .dependsOn(core)
  .settings(name := "freestyle-effects")
  .jsSettings(sharedJsSettings: _*)

lazy val effectsJVM = effects.jvm
lazy val effectsJS  = effects.js

lazy val async = (crossProject in file("async/async"))
  .dependsOn(core)
  .settings(name := "freestyle-async")
  .jsSettings(sharedJsSettings: _*)

lazy val asyncJVM = async.jvm
lazy val asyncJS  = async.js

lazy val asyncMonix = (crossProject in file("async/monix"))
  .dependsOn(core, async)
  .settings(name := "freestyle-async-monix")
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix-eval" % v("monix"),
      "io.monix" %%% "monix-cats" % v("monix")
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val asyncMonixJVM = asyncMonix.jvm
lazy val asyncMonixJS  = asyncMonix.js

lazy val asyncFs2 = (crossProject in file("async/fs2"))
  .dependsOn(core, async)
  .settings(name := "freestyle-async-fs2")
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-core" % v("fs2"),
      "co.fs2" %%% "fs2-cats" % v("fs2-cats")
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val asyncFs2JVM = asyncFs2.jvm
lazy val asyncFs2JS  = asyncFs2.js

lazy val cache = (crossProject in file("freestyle-cache"))
  .dependsOn(core)
  .settings(name := "freestyle-cache")
  .jsSettings(sharedJsSettings: _*)

lazy val cacheJVM = cache.jvm
lazy val cacheJS  = cache.js

lazy val cacheRedis = (project in file("freestyle-cache-redis"))
  .dependsOn(coreJVM, cacheJVM)
  .settings(name := "freestyle-cache-redis")
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
  .dependsOn(coreJVM)
  .settings(name := "freestyle-doobie")
  .settings(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core-cats" % v("doobie"),
      "org.tpolecat" %% "doobie-h2-cats"   % v("doobie")
    )
  )

lazy val slick = (project in file("freestyle-slick"))
  .dependsOn(coreJVM, asyncJVM)
  .settings(name := "freestyle-slick")
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % v("slick"),
      "com.h2database"      % "h2"    % "1.4.194" % "test"
    )
  )

lazy val twitterUtil = (project in file("freestyle-twitter-util"))
  .dependsOn(coreJVM)
  .settings(name := "freestyle-twitter-util")
  .settings(
    libraryDependencies += "io.catbird" %% "catbird-util" %v("catbird")
  )

lazy val fixResources = taskKey[Unit]("Fix application.conf presence on first clean build.")

lazy val config = (project in file("freestyle-config"))
  .dependsOn(coreJVM)
  .settings(name := "freestyle-config")
  .settings(
    fixResources := {
      val testConf = (resourceDirectory in Test).value / "application.conf"
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
    libraryDependencies += "com.typesafe" % "config" % v("config")
  )

lazy val fetch = (crossProject in file("freestyle-fetch"))
  .dependsOn(core)
  .settings(name := "freestyle-fetch")
  .settings(
    libraryDependencies ++= Seq(
      "com.47deg" %%% "fetch"       % v("fetch"),
      "com.47deg" %%% "fetch-monix" % v("fetch")
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val fetchJVM = fetch.jvm
lazy val fetchJS  = fetch.js

lazy val logging = (crossProject in file("freestyle-logging"))
  .dependsOn(core)
  .settings(name := "freestyle-logging")
  .jvmSettings(
    libraryDependencies += "io.verizon.journal" %% "core" % v("journal")
  )
  .jsSettings(
    libraryDependencies += "biz.enef" %%% "slogging" % v("slogging")
  )
  .jsSettings(sharedJsSettings: _*)

lazy val loggingJVM = logging.jvm
lazy val loggingJS  = logging.js

lazy val fs2 = (crossProject in file("freestyle-fs2"))
  .dependsOn(core)
  .settings(name := "freestyle-fs2")
  .settings(
    libraryDependencies += "co.fs2" %%% "fs2-core" % v("fs2")
  )
  .jsSettings(sharedJsSettings: _*)

lazy val fs2JVM = fs2.jvm
lazy val fs2JS  = fs2.js

lazy val httpHttp4s = (project in file("http/http4s"))
  .dependsOn(coreJVM)
  .settings(name := "freestyle-http-http4s")
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % v("http4s"),
      "org.http4s" %% "http4s-dsl"  % v("http4s") % "test"
    )
  )

lazy val httpFinch = (project in file("http/finch"))
  .dependsOn(coreJVM)
  .settings(name := "freestyle-http-finch")
  .settings(
    libraryDependencies += "com.github.finagle" %% "finch-core" % v("finch")
  )

lazy val httpAkka = (project in file("http/akka"))
  .dependsOn(coreJVM)
  .settings(name := "freestyle-http-akka")
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"         % v("akka-http"),
      "com.typesafe.akka" %% "akka-http-testkit" % v("akka-http") % "test"
    )
  )

lazy val httpPlay = (project in file("http/play"))
  .dependsOn(coreJVM)
  .settings(name := "freestyle-http-play")
  .settings(
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play"      % v("play"),
      "com.typesafe.play" %% "play-test" % v("play") % "test"
    )
  )

lazy val tests = (project in file("tests"))
  .dependsOn(coreJVM)
  .settings(noPublishSettings: _*)
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

lazy val docs = (project in file("docs"))
  .dependsOn(coreJVM, effectsJVM, fs2JVM, fetchJVM, cacheJVM, loggingJVM, asyncFs2JVM, asyncMonixJVM)
  .dependsOn(doobie, slick, httpPlay, httpHttp4s, httpFinch, config)
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := "docs",
    description := "freestyle docs"
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % v("http4s")
    )
  )
  .enablePlugins(MicrositesPlugin)

orgAfterCISuccessTaskListSetting := List(
  orgCreateFiles.toOrgTask,
  orgCommitPolicyFiles.toOrgTask,
  depUpdateDependencyIssues.toOrgTask,
  (publishMicrosite in docs).toOrgTask,
  orgPublishRelease.toOrgTask(allModulesScope = true, crossScalaVersionsScope = true)
)
