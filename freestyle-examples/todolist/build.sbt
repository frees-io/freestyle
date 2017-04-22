name := "freestyle-todolist"

version := "1.0"

scalaVersion := "2.12.2"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)

val catsVersion = "0.9.0"
val circeVersion = "0.7.0"
val doobieVersion = "0.4.1"
val finchVersion = "0.14.0"
val fs2Version = "0.9.5"
val freestyleVersion = "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % catsVersion,
  "io.circe" %% "circe-generic" % circeVersion,

  "org.tpolecat" %% "doobie-core-cats" % doobieVersion,
  "org.tpolecat" %% "doobie-h2-cats" % doobieVersion,

  "com.github.finagle" %% "finch-core" % finchVersion,
  "com.github.finagle" %% "finch-circe" % finchVersion,

  "co.fs2" %% "fs2-core" % fs2Version,

  "com.47deg" %% "freestyle" % freestyleVersion,
  "com.47deg" %% "freestyle-doobie" % freestyleVersion,
  "com.47deg" %% "freestyle-http-finch" % freestyleVersion
)

