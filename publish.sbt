organizationName := "47 Degrees"

organizationHomepage := Some(new URL("http://47deg.com"))

publishMavenStyle := true

startYear := Some(2016)

description := "Freestyle is a library to help building libraries and applications based on Free monads."

homepage := Some(url("http://47deg.com"))

scmInfo := Some(ScmInfo(url("https://github.com/47deg/freestyle"), "https://github.com/47deg/freestyle.git"))

licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

pomExtra :=
    <developers>
      <developer>
        <name>47 Degrees (twitter: @47deg)</name>
        <email>hello@47deg.com</email>
      </developer>
      <developer>
        <name>Raul Raja Martinez (twitter: @raulraja)</name>
        <email>raulraja@gmail.com</email>
      </developer>
    </developers>

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

lazy val gpgFolder = sys.env.getOrElse("GPG_FOLDER", ".")

pgpPassphrase := Some(sys.env.getOrElse("GPG_PASSPHRASE", "").toCharArray)

pgpPublicRing := file(s"$gpgFolder/pubring.gpg")

pgpSecretRing := file(s"$gpgFolder/secring.gpg")

credentials += Credentials("Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  sys.env.getOrElse("PUBLISH_USERNAME", ""),
  sys.env.getOrElse("PUBLISH_PASSWORD", ""))

publishArtifact in Test := false

lazy val publishSnapshot = taskKey[Unit]("Publish only if the version is a SNAPSHOT")

publishSnapshot := Def.taskDyn {
  if (isSnapshot.value) Def.task { PgpKeys.publishSigned.value }
  else Def.task(println("Actual version is not a Snapshot. Skipping publish."))
}.value
