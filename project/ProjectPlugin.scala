import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._
import microsites.MicrositeKeys._
import sbt.Keys._
import sbt._
import sbtorgpolicies.OrgPoliciesKeys.orgBadgeListSetting
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model._
import sbtorgpolicies.runnable.SetSetting
import sbtorgpolicies.templates.badges._
import sbtorgpolicies.runnable.syntax._
import scoverage.ScoverageKeys
import scoverage.ScoverageKeys._
import tut.Plugin._

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
      )
    )

    lazy val commonDeps: Seq[ModuleID] = Seq(%%("scalatest") % "test")
  }

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      description := "A Cohesive & Pragmatic Framework of FP centric Scala libraries",
      startYear := Some(2017),
      orgProjectName := "Freestyle",
      orgBadgeListSetting := List(
        TravisBadge.apply,
        CodecovBadge.apply,
        MavenCentralBadge.apply,
        ScalaLangBadge.apply,
        LicenseBadge.apply,
        GitterBadge.apply,
        GitHubIssuesBadge.apply,
        ScalaJSBadge.apply
      ),
      orgSupportedScalaJSVersion := Some("0.6.15"),
      orgScriptTaskListSetting := List(
        orgValidateFiles.asRunnableItem,
        (clean in Global).asRunnableItemFull,
        SetSetting(coverageEnabled in Global, true).asRunnableItem,
        (compile in Compile).asRunnableItemFull,
        (test in Test).asRunnableItemFull,
        (ScoverageKeys.coverageReport in Test).asRunnableItemFull,
        (ScoverageKeys.coverageAggregate in Test).asRunnableItemFull,
        SetSetting(coverageEnabled in Global, false).asRunnableItem
      ) ++ guard(scalaBinaryVersion.value == "2.12")(
        (tut in ProjectRef(file("."), "docs")).asRunnableItem),
      resolvers += Resolver.sonatypeRepo("snapshots"),
      scalacOptions ++= scalacAdvancedOptions,
      scalacOptions ~= (_ filterNot Set("-Yliteral-types", "-Xlint").contains),
      parallelExecution in Test := false,
      compileOrder in Compile := CompileOrder.JavaThenScala,
      coverageFailOnMinimum := false
    ) ++ scalaMacroDependencies
}
