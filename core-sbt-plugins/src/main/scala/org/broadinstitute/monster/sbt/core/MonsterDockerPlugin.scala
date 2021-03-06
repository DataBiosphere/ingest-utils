package org.broadinstitute.monster.sbt.core

import com.typesafe.sbt.packager.NativePackagerKeys
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.linux.LinuxKeys
import sbt.Keys._
import sbt._

/**
  * Plugin which should be applied to Monster sub-projects that need to be
  * published as Docker images for use in pipelines.
  */
object MonsterDockerPlugin extends AutoPlugin with LinuxKeys with NativePackagerKeys {

  import DockerPlugin.autoImport._

  override def requires = DockerPlugin && JavaAppPackaging && MonsterBasePlugin

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      dockerBaseImage := "ghcr.io/graalvm/graalvm-ce:ol7-java8-20.3.1",
      dockerRepository := Some("us.gcr.io/broad-dsp-gcr-public"),
      dockerLabels := Map("VERSION" -> version.value),
      Docker / defaultLinuxInstallLocation := "/app",
      Docker / maintainer := "monster@broadinstitute.org",
      // sbt-native-packager tries to do a good thing by generating a non-root user
      // to run the container, but so many systems assume images are running as root
      // that using a different user ends up breaking things (i.e. k8s persistent
      // volumes are chmod-ed to be writeable only by "root").
      Docker / daemonUserUid := None,
      Docker / daemonUser := "root",
      // Publish "latest" tag to make coordinating versions in dev simpler.
      dockerUpdateLatest := true,
      // Make our CI life easier and set up publish delegation here.
      publish := (Docker / publish).value,
      publishLocal := (Docker / publishLocal).value
    )
}
