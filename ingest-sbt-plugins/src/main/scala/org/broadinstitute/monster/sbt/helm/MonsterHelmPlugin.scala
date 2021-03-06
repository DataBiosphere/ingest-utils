package org.broadinstitute.monster.sbt.helm

import org.broadinstitute.monster.sbt.core.MonsterBasePlugin
import sbt._
import sbt.Keys._
import sbt.nio.Keys._

import scala.collection.mutable
import scala.sys.process.Process
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Plugin for projects which wrap a Helm chart.
  *
  * Managing Helm in sbt is kinda a stretch. The main benefits are:
  *   1. Being able to inject the "official" version of the build into chart metadata
  *   2. Being able to run a single "publish" command and have charts get pushed
  * along-side libraries and Docker images
  */
object MonsterHelmPlugin extends AutoPlugin {

  override def requires: Plugins = MonsterBasePlugin

  object autoImport extends MonsterHelmPluginKeys

  import autoImport._

  // Assumes chart-releaser is available on the local PATH,
  // and that CR_TOKEN is in the environment.
  def cr(cmd: String, args: (String, String)*): Unit = {
    val fullCommand =
      "cr" :: cmd :: args.toList.flatMap { case (k, v) => List(k, v) }
    val result = Process(fullCommand).!
    if (result != 0) {
      sys.error(s"`cr $cmd` failed with exit code $result")
    }
  }

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      // Test-related settings.
      Test / helmExampleValuesSource := baseDirectory.value / "example-values",
      Test / lintHelmChart := {
        val log = streams.value.log
        val chart = baseDirectory.value
        val examples = (Test / helmExampleValuesSource).value.glob("*.yaml")

        val failedExamples = new mutable.ArrayBuffer[String]
        examples.get().foreach { example =>
          val exampleName = example.getName
          log.info(s"Attempting to render chart with values: $exampleName")
          try {
            Helm.clp.lintChart(chart, example)
          } catch {
            case NonFatal(_) => failedExamples.append(exampleName)
          }
        }
        if (failedExamples.nonEmpty) {
          sys.error(s"Linting failed for example(s): ${failedExamples.mkString(", ")}")
        }
      },
      test := (Test / lintHelmChart).value,
      // Publish-related settings.
      helmStagingDirectory := target.value / "helm" / "packaged",
      helmChartLocalIndex := target.value / "helm" / "index.yaml",
      helmInjectVersionValues := { (baseValues, _) => baseValues },
      packageHelmChart / fileInputs += baseDirectory.value.toGlob / **,
      packageHelmChart := {
        val inputChanges = packageHelmChart.inputFileChanges
        val targetDir = target.value.toPath
        val filteredChanges = List
          .concat(
            inputChanges.created,
            inputChanges.modified,
            inputChanges.deleted
          )
          .filterNot(_.startsWith(targetDir))

        val packageTarget = helmStagingDirectory.value
        IO.createDirectory(packageTarget)
        if (filteredChanges.nonEmpty || packageTarget.list().isEmpty) {
          Helm.clp.packageChart(baseDirectory.value, version.value, packageTarget)(
            helmInjectVersionValues.value
          )
        }
        packageTarget
      },
      releaseHelmChart := {
        val log = streams.value.log
        val packageDir = packageHelmChart.value.getAbsolutePath()
        val org = helmChartOrganization.value
        val repo = helmChartRepository.value

        log.info(s"Uploading Helm chart for ${name.value} to $org/$repo...")
        cr("upload", "-o" -> org, "-r" -> repo, "-p" -> packageDir)
      },
      reindexHelmRepository := {
        val log = streams.value.log
        val packageDir = packageHelmChart.value.getAbsolutePath()
        val org = helmChartOrganization.value
        val repo = helmChartRepository.value
        val indexTarget = helmChartLocalIndex.value

        val indexSite = s"https://$org.github.io/$repo"
        log.info(s"Adding Helm chart for ${name.value} to index for $indexSite...")
        IO.createDirectory(indexTarget.getParentFile())
        cr(
          "index",
          "-c" -> indexSite,
          "-o" -> org,
          "-r" -> repo,
          "-p" -> packageDir,
          "-i" -> indexTarget.getAbsolutePath()
        )

        val gitBase = (ThisBuild / baseDirectory).value

        def git(cmd: String, args: String*): Unit = {
          val fullCommand = Seq("git", cmd) ++ args
          val result = Process(fullCommand, gitBase).!
          if (result != 0) {
            sys.error(s"`git $cmd` failed with exit code $result")
          }
        }

        def gitRead(cmd: String, args: String*): String = {
          val fullCommand = Seq("git", cmd) ++ args
          Process(fullCommand, gitBase).!!.trim()
        }

        val currentBranch = gitRead("rev-parse", "--abbrev-ref", "HEAD")

        log.info(s"Pushing updated index to $indexSite...")
        val attemptPushingIndex = Try {
          git("checkout", "gh-pages")
          IO.copyFile(indexTarget, gitBase / "index.yaml")

          val diff = gitRead("status", "--porcelain")
          if (diff.isEmpty) {
            log.info(s"Index for $indexSite is unchanged")
          } else {
            git("add", "index.yaml")
            git("commit", "-m", "Update index.")
            git("push", "-f", "origin", "gh-pages")
          }
        }

        attemptPushingIndex match {
          case Success(_) =>
            log.info(s"Successfully pushed index to $indexSite")
            git("checkout", currentBranch)
          case Failure(exception) =>
            git("checkout", currentBranch)
            sys.error(s"Failed to push index to $indexSite: ${exception.getMessage}")
        }
      },
      publish := Def.taskDyn {
        if (isSnapshot.value) {
          // Don't bother publishing SNAPSHOT releases, since we can
          // always sync w/ Flux instead.
          Def.task {}
        } else {
          releaseHelmChart.toTask
        }
      }.value,
      // Doesn't make sense to publish a Helm chart locally(?)
      publishLocal := {}
    )
}
