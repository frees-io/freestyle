resolvers += ("snapshots" at
  "https://oss.sonatype.org/content/repositories/snapshots")
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.fortysevendeg" % "sbt-catalysts-extras" % "0.1.3")

addSbtPlugin("com.fortysevendeg" % "sbt-microsites" % "0.6.0-SNAPSHOT")

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "0.4.10")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.14")
