import freestyle.FreestylePlugin
import sbt._
import sbt.Keys._
import sbtorgpolicies.OrgPoliciesPlugin.autoImport.orgScriptTaskListSetting
import sbtorgpolicies.runnable.syntax._
import scoverage.ScoverageKeys.coverageExcludedFiles

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = FreestylePlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    def toCompileTestList(sequence: Seq[ProjectReference]): List[String] = sequence.toList.map {
      p =>
        val project: String = p.asInstanceOf[LocalProject].project
        s"$project/test"
    }

  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    coverageExcludedFiles in Global := ".*<macro>",
    orgScriptTaskListSetting := List("validate".asRunnableItemFull),
    packageDoc in Compile := {
      val sourceFile = (baseDirectory in LocalRootProject).value / "README.md"
      val targetFile = crossTarget.value / s"${name.value}-javadoc.jar"

      IO.copy(Seq(sourceFile -> targetFile), overwrite = true).toSeq

      targetFile
    }
  )

}
