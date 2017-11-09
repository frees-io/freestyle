import freestyle.FreestylePlugin
import org.scalajs.sbtplugin.cross.{CrossProject, CrossType}
import sbt._
import sbt.Keys._
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.runnable.syntax._
import scoverage.ScoverageKeys._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.ReleasePlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = FreestylePlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    def module(
        modName: String,
        hideFolder: Boolean = false,
        full: Boolean = true,
        subFolder: Option[String] = None): CrossProject = {
      val folderPath =
        s"""modules${subFolder.fold("")(sf => s"/$sf")}/${if (hideFolder) "." else ""}$modName"""
      CrossProject(
        modName,
        file(folderPath),
        if (full) CrossType.Full else CrossType.Pure
      ).settings(moduleName := s"frees-$modName")
    }

    def jvmModule(modName: String, subFolder: Option[String] = None): Project = {
      val folderPath = s"""modules${subFolder.fold("")(sf => s"/$sf")}/$modName"""
      Project(modName, file(folderPath))
        .settings(moduleName := s"frees-$modName")
    }

    lazy val slickGen = TaskKey[Seq[File]]("slick-gen")

    lazy val slickCodeGenTask = Def.task {
      val outputDir = (sourceDirectory.value / "main/scala").getPath
      (runner in Compile).value.run(
        "slick.codegen.SourceCodeGenerator",
        (dependencyClasspath in Compile).value.files,
        Array(
          "slick.jdbc.PostgresProfile",
          "org.postgresql.Driver",
          "jdbc:postgresql://localhost/postgres?currentSchema=public",
          outputDir,
          "freeslick.dao",
          "test",
          "test"
        ),
        streams.value.log
      )
      Seq(file(s"$outputDir/dao/Tables.scala"))
    }
  }

  /**
   * Custom release process, since tests are being
   * executed in a previous stage (see Travis pipeline).
   */
  lazy val sharedReleaseProcess = Seq(
    releaseProcess := Seq[ReleaseStep](
      orgInitialVcsChecks,
      checkSnapshotDependencies,
      orgInquireVersions,
      runClean,
      orgTagRelease,
      orgUpdateChangeLog,
      if (sbtPlugin.value) releaseStepCommandAndRemaining("^ publishSigned") else publishArtifacts,
      setNextVersion,
      orgCommitNextVersion,
      ReleaseStep(action = "sonatypeReleaseAll" :: _),
      orgPostRelease
    )
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      orgUpdateDocFilesSetting += baseDirectory.value / "docs" / "src",
      orgScriptTaskListSetting := List("validate".asRunnableItemFull),
      coverageExcludedPackages := "<empty>;todo\\..*;freeslick\\..*"
    ) ++ scalaMetaSettings ++ sharedReleaseProcess

}
