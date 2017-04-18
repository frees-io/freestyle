import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._
import dependencies.DependenciesPlugin.autoImport.depUpdateDependencyIssues
import microsites.MicrositeKeys._
import microsites.MicrositesPlugin.autoImport.publishMicrosite
import microsites.util.BuildHelper.buildWithoutSuffix
import sbt.Keys._
import sbt._
import sbtorgpolicies.OrgPoliciesKeys.orgBadgeListSetting
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model._
import sbtorgpolicies.templates.badges._
import scoverage.ScoverageSbtPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = OrgPoliciesPlugin

  object autoImport {

    lazy val fixResources: TaskKey[Unit] =
      taskKey[Unit]("Fix application.conf presence on first clean build.")

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

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      coverageMinimum := 80,
      coverageFailOnMinimum := false,
      description := "A Cohesive & Pragmatic Framework of FP centric Scala libraries",
      startYear := Some(2017),
      orgGithubTokenSetting := "GITHUB_TOKEN_REPO",
      orgBadgeListSetting := List(
        TravisBadge.apply,
        CodecovBadge.apply,
        LicenseBadge.apply,
        GitterBadge.apply,
        GitHubIssuesBadge.apply,
        ScalaJSBadge.apply
      ),
      resolvers += Resolver.sonatypeRepo("snapshots"),
      scalacOptions ++= scalacAdvancedOptions,
      parallelExecution in Test := false,
      compileOrder in Compile := CompileOrder.JavaThenScala
    ) ++ scalaMacroDependencies
}
