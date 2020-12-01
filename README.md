# Ingest Utilities
Build plugins and libraries used by the Broad's Data Ingest and Modeling team
when building ETL ingest pipelines for flagship datasets.

## Motivation
Over time, the Scala community has accumulated a set of best-practice settings
for configuring `sbt` builds. On top of that, the Data Ingest team has built up an
opinionated set of configs and helper methods we want to be applied to our builds.
This repository provides a centralized location to define & version these combined
settings and ETL helpers.

## Why plugins?
Many options exist for sharing common code across `sbt` projects. The
[`giter8`](http://www.foundweekends.org/giter8/) is `sbt`-specific, and allows
for template builds to be imported and "filled in" to create new projects.
GitHub also provides support for generic
[template repositories](https://github.blog/2019-06-06-generate-new-repositories-with-repository-templates/).

These alternatives are simpler to publish than `sbt` plugins and provide
the same value when creating new projects. However, once a project has been
created using either of these methods, any updates to shared build settings
must be done manually. `sbt` plugins provide the long-term value of being able
to bump a single version number to pull in the latest settings updates, without
needing to know exactly what the updates might be.

## Installing plugins
Add the following to a project's `project/plugins.sbt` to install Monster's `sbt` plugins:
```sbt
val patternBase =
  "org/broadinstitute/monster/[module](_[scalaVersion])(_[sbtVersion])/[revision]"

val publishPatterns = Patterns()
  .withIsMavenCompatible(false)
  .withIvyPatterns(Vector(s"$patternBase/ivy-[revision].xml"))
  .withArtifactPatterns(Vector(s"$patternBase/[module]-[revision](-[classifier]).[ext]"))

resolvers += Resolver.url(
  "Broad Artifactory",
  new URL("https://broadinstitute.jfrog.io/broadinstitute/libs-release/")
)(publishPatterns)

addSbtPlugin("org.broadinstitute.monster" % "ingest-sbt-plugins" % "<version>")
// If your project isn't generating data for a Jade dataset, you can just do:
//addSbtPlugin("org.broadinstitute.monster" % "core-sbt-plugins" % "<version>")
```

Eventually we intend to publish a higher-level template repository containing this boilerplate.

## Available plugins
| Plugin Name | Artifact | Description |
| ----------- | ----------- | ----------- |
| `MonsterBasePlugin` | `core-sbt-plugins` | Core settings for compilation, formatting, versioning, and test coverage. |
| `MonsterLibraryPlugin` | `core-sbt-plugins` | Settings for publishing to Broad's Artifactory instance. |
| `MonsterDockerPlugin` | `core-sbt-plugins` | Settings for publishing to DSP's public GCR repository. |
| `MonsterJadeDatasetPlugin` | `ingest-sbt-plugins` | Settings for working with Jade datasets and publishing schema images to GCR. |
| `MonsterScioPipelinePlugin` | `ingest-sbt-plugins` | Settings for developing Scio projects and publishing runner images to GCR. |
| `MonsterHelmPlugin` | `ingest-sbt-plugins` | Settings for developing Helm charts and publishing charts to GitHub pages. |

See the individual plugin directories for more documentation about each artifact.

## Publishing a new version
This repo uses git to manage releases. Every merge to `master` is published
to Broad's snapshot repository, using the preceding version and commit history
to construct a version number. Commits tagged with a string that matches the
pattern `v<semver-version>` are re-published to the releases repository under
the associated version.

There is no automated process for deciding the next version number, or for pushing
the tag. Make a value judgement on whether to bump the `major`, `minor`, or `patch`
component of the version, then run:
```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```
GitHub will pick up the new tag and run the publish operation.
