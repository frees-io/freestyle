resolvers += ("snapshots" at
  "https://oss.sonatype.org/content/repositories/snapshots")
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.47deg" % "sbt-org-policies" % "0.2.1")

addSbtPlugin("com.fortysevendeg" % "sbt-microsites" % "0.5.0-SNAPSHOT")
