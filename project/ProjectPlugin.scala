import freestyle.FreestylePlugin
import org.scalajs.sbtplugin.cross.{CrossProject, CrossType}
import sbt._
import sbt.Keys.moduleName
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.runnable.syntax._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = FreestylePlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    def module(modName: String, hideFolder: Boolean = false, full: Boolean = true): CrossProject =
      CrossProject(
        modName, 
        file(s"""modules/${if (hideFolder) "." else ""}$modName"""), 
        if (full) CrossType.Full else CrossType.Pure
      ).settings(moduleName := s"frees-$modName")

    def jvmModule(modName: String): Project =
      Project(modName, file(s"""modules/$modName"""))
        .settings(moduleName := s"frees-$modName")

  }

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      orgScriptTaskListSetting := List("validate".asRunnableItemFull)
    ) ++ scalaMetaSettings

}
