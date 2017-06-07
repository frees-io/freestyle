resolvers ++= Seq(Resolver.sonatypeRepo("snapshots"), Resolver.sonatypeRepo("releases"))
addSbtPlugin("io.frees" % "sbt-freestyle" % "0.0.1-SNAPSHOT" changing())

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.0.3")
