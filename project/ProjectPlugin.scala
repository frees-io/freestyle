import sbt.Keys._
import sbt._
import microsites.MicrositeKeys._
import microsites.util.BuildHelper.buildWithoutSuffix
import sbtorgpolicies._
import sbtorgpolicies.model._
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = OrgPoliciesPlugin

  object autoImport {

    lazy val micrositeSettings = Seq(
      micrositeName := "Freestyle",
      micrositeDescription := "A Cohesive & Pragmatic Framework of FP centric Scala libraries",
      micrositeDocumentationUrl := "/docs/",
      micrositeGithubOwner := "47deg",
      micrositeGithubRepo := "freestyle",
      micrositeHighlightTheme := "dracula",
      micrositeExternalLayoutsDirectory := (resourceDirectory in Compile).value / "microsite" / "layouts",
      micrositeExternalIncludesDirectory := (resourceDirectory in Compile).value / "microsite" / "includes",
      includeFilter in Jekyll := ("*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md" | "CNAME"),
      micrositePalette := Map(
        "brand-primary"   -> "#01C2C2",
        "brand-secondary" -> "#142236",
        "brand-tertiary"  -> "#202D40",
        "gray-dark"       -> "#383D44",
        "gray"            -> "#646D7B",
        "gray-light"      -> "#E6E7EC",
        "gray-lighter"    -> "#F4F5F9",
        "white-color"     -> "#E6E7EC"
      ),
      micrositeKazariCodeMirrorTheme := "dracula",
      micrositeKazariDependencies := Seq(
        microsites.KazariDependency(
          "com.47deg",
          "freestyle",
          buildWithoutSuffix(scalaVersion.value),
          version.value),
        microsites.KazariDependency("org.scalamacros", "paradise", scalaVersion.value, "2.1.0")
      ),
      micrositeKazariResolvers := Seq(
        "https://oss.sonatype.org/content/repositories/snapshots",
        "https://oss.sonatype.org/content/repositories/releases")
    )
  }

  import scoverage.ScoverageSbtPlugin.autoImport._

  override def projectSettings =
    Seq(
      coverageMinimum := 80,
      coverageFailOnMinimum := false,
      description := "A Cohesive & Pragmatic Framework of FP centric Scala libraries",
      onLoad := (Command.process("project freestyle", _: State)) compose (onLoad in Global).value,
      scalacOptions ++= scalacAdvancedOptions,
      libraryDependencies += %%("scalatest") % "test",
      parallelExecution in Test := false
    ) ++ scalaMacroDependencies
}
