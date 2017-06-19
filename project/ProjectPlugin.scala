import freestyle.FreestylePlugin
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin.autoImport.orgScriptTaskListSetting
import sbtorgpolicies.runnable.syntax._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = FreestylePlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    orgScriptTaskListSetting := List("validate".asRunnableItemFull)
  )

}
