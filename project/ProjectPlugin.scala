import freestyle.FreestylePlugin
import org.scalajs.sbtplugin.cross.{CrossProject, CrossType}
import sbt._
import sbt.Keys._
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.runnable.syntax._

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

  }

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      orgUpdateDocFilesSetting += baseDirectory.value / "docs" / "src",
      orgScriptTaskListSetting := List("validate".asRunnableItemFull) ++ guard(
        scalaBinaryVersion.value == "2.12")("tut".asRunnableItem)
    ) ++ scalaMetaSettings

}
