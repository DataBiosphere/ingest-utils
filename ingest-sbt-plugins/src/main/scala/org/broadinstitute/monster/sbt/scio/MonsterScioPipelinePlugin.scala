package org.broadinstitute.monster.sbt.scio

import com.typesafe.sbt.packager.universal.UniversalPlugin
import org.broadinstitute.monster.buildinfo.IngestSbtPluginsBuildInfo
import org.broadinstitute.monster.sbt.core.MonsterDockerPlugin
import sbt._
import sbt.Keys._

/** Plugin for projects which build a Scio ETL pipeline. */
object MonsterScioPipelinePlugin extends AutoPlugin {
  override def requires: Plugins = MonsterDockerPlugin

  private def scioUtilsDep(module: String): ModuleID =
    "org.broadinstitute.monster" %% module % IngestSbtPluginsBuildInfo.version

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    // Set best-practice compiler flags for Scio.
    scalacOptions ++= Seq(
      "-Xmacro-settings:show-coder-fallback=true",
      "-language:higherKinds"
    ),
    // Add our common utils library.
    libraryDependencies ++= Seq(
      scioUtilsDep(IngestSbtPluginsBuildInfo.scioUtilsName),
      scioUtilsDep(IngestSbtPluginsBuildInfo.scioTestUtilsName) % s"${Test.name},${IntegrationTest.name}"
    ),
    // Disable scio's annoying automatic version check.
    javaOptions += "-Dscio.ignoreVersionWarning=true",
    UniversalPlugin.autoImport.Universal / javaOptions += "-Dscio.ignoreVersionWarning=true"
  )
}
