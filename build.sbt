addCommandAlias("debug", "; clean ; test")

addCommandAlias("validate", "; +clean ; +test; makeMicrosite")

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val freestyle = (crossProject in file("freestyle"))
  .settings(name := "freestyle")
  .settings(
    libraryDependencies ++= Seq(
      %("scala-reflect", scalaVersion.value),
      %%%("cats-free"),
      %%%("shapeless"),
      %%("monix-eval") % "test",
      %%("monix-cats") % "test"
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleJVM = freestyle.jvm
lazy val freestyleJS  = freestyle.js

lazy val freestyleMonix = (crossProject in file("freestyle-monix"))
  .settings(name := "freestyle-monix")
  .settings(
    libraryDependencies ++= Seq(
      %%%("monix-eval"),
      %%%("monix-cats")
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleMonixJVM = freestyleMonix.jvm
lazy val freestyleMonixJS  = freestyleMonix.js

lazy val freestyleEffects = (crossProject in file("freestyle-effects"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-effects")
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleEffectsJVM = freestyleEffects.jvm
lazy val freestyleEffectsJS  = freestyleEffects.js

lazy val freestyleAsync = (crossProject in file("async/async"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-async")
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleAsyncJVM = freestyleAsync.jvm
lazy val freestyleAsyncJS  = freestyleAsync.js

lazy val freestyleAsyncMonix = (crossProject in file("async/monix"))
  .dependsOn(freestyle, freestyleAsync)
  .settings(name := "freestyle-async-monix")
  .settings(
    libraryDependencies ++= Seq(
      %%("monix-eval"),
      %%("monix-cats")
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleAsyncMonixJVM = freestyleAsyncMonix.jvm
lazy val freestyleAsyncMonixJS  = freestyleAsyncMonix.js

lazy val freestyleAsyncFs = (crossProject in file("async/fs2"))
  .dependsOn(freestyle, freestyleAsync)
  .settings(name := "freestyle-async-fs2")
  .settings(
    libraryDependencies ++= Seq(
      %%%("fs2-core"),
      %%%("fs2-cats")
    )
  )

lazy val freestyleAsyncFsJVM = freestyleAsyncFs.jvm
lazy val freestyleAsyncFsJS  = freestyleAsyncFs.js

lazy val freestyleCache = (crossProject in file("freestyle-cache"))
  .dependsOn(freestyle)
  .settings(
    name := "freestyle-cache"
  )
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleCacheJVM = freestyleCache.jvm
lazy val freestyleCacheJS  = freestyleCache.js

lazy val freestyleCacheRedis = (crossProject in file("freestyle-cache-redis"))
  .dependsOn(freestyle, freestyleCache)
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
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleCacheRedisJVM = freestyleCacheRedis.jvm

lazy val freestyleDoobie = (project in file("freestyle-doobie"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-doobie")
  .settings(
    libraryDependencies ++= Seq(
      %%("doobie-core-cats"),
      %%("doobie-h2-cats") % "test"
    )
)

lazy val freestyleSlick = (project in file("freestyle-slick"))
  .dependsOn(freestyleJVM, freestyleAsyncJVM)
  .settings(name := "freestyle-slick")
  .settings(
    libraryDependencies ++= Seq(
      %%("slick"),
      "com.h2database" % "h2" % "1.4.194" % "test"
    )
  )

lazy val freestyleTwitterUtil = (project in file("freestyle-twitter-util")).
  dependsOn(freestyleJVM).
  settings(name := "freestyle-twitter-util").
  settings(
    libraryDependencies ++= Seq(
      %%("catbird-util")
    )
  )

lazy val fixResources = taskKey[Unit]("Fix application.conf presence on first clean build.")

lazy val freestyleConfig = (crossProject in file("freestyle-config"))
  .dependsOn(freestyle)
  .settings(
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
  )
  .settings(
    libraryDependencies ++= Seq(
      %%%("shocon")
    )
  )

lazy val freestyleConfigJVM = freestyleConfig.jvm
lazy val freestyleConfigJS  = freestyleConfig.js

lazy val freestyleFetch = (crossProject in file("freestyle-fetch"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-fetch")
  .settings(
    libraryDependencies ++= Seq(
      %%%("fetch"),
      %%%("fetch-monix")
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleFetchJVM = freestyleFetch.jvm
lazy val freestyleFetchJS  = freestyleFetch.js

lazy val freestyleLogging = (crossProject in file("freestyle-logging"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-logging")
  .jvmSettings(
    libraryDependencies += %%("journal-core")
  )
  .jsSettings(
    libraryDependencies += %%%("slogging")
  )
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleLoggingJVM = freestyleLogging.jvm
lazy val freestyleLoggingJS  = freestyleLogging.js

lazy val freestyleFs2 = (crossProject in file("freestyle-fs2"))
  .dependsOn(freestyle)
  .settings(name := "freestyle-fs2")
  .settings(
    libraryDependencies ++= Seq(
      %%%("fs2-core")
    )
  )
  .jsSettings(sharedJsSettings: _*)

lazy val freestyleFs2JVM = freestyleFs2.jvm
lazy val freestyleFs2JS  = freestyleFs2.js

lazy val freestylePlay = (project in file("http/play"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-play")
  .settings(
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      %%("play"),
      %%("play-test") % "test"
    )
  )

lazy val tests = (project in file("tests"))
  .dependsOn(freestyleJVM)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
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
  .dependsOn(freestyleJVM)
  .dependsOn(freestyleEffectsJVM)
  .dependsOn(freestyleFs2JVM)
  .dependsOn(freestyleFetchJVM)
  .dependsOn(freestyleCacheJVM)
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := "docs",
    description := "freestyle docs"
  )
  .settings(
    libraryDependencies ++= Seq(
      %%("fs2-io"),
      %%("fs2-cats")
    )
  )
  .enablePlugins(MicrositesPlugin)

lazy val freestyleHttpHttp4s = (project in file("http/http4s"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-http4s")
  .settings(
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= Seq(
      %%("http4s-core"),
      %%("http4s-dsl") % "test"
    )
  )

lazy val freestyleHttpFinch = (project in file("http/finch"))
  .dependsOn(freestyleJVM)
  .settings(name := "freestyle-http-finch")
  .settings(
    libraryDependencies ++= Seq(
      %%("finch-core")
    )
  )
