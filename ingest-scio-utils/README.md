# Scio Utilities
This project defines common functionality used across the Scio components
of our ETL pipelines.

## Motivation
We find there are a few patterns that repeat across the Scio pipelines
used to transform data during ingest. Centralizing those patterns into
a library helps us keep behavior consistent and test core functionality
in isolation.

## Using the library
This library is published to Broad's Artifactory instance. Any project
which enables the `MonsterBasePlugin` should be configured to pull dependencies
from that repository using:
```sbt
libraryDependencies += "org.broadinstitute.monster" %% "ingest-scio-utils" % "<version>"
```

## Upgrading Scio / Beam
When running a job in Dataflow, you might see a notification on the right-hand side
which says a new version of the Beam SDK is available. Alternatively, you might
encounter a bug in Scio, then see it's been fixed in a later version. In either case,
it's not usually enough to bump one library or the other; Scio encodes deep dependencies
on Beam, so they usually need to be upgraded in lock-step.

Regardless of which library is driving the upgrade, you should start at the
[Scio releases page](https://github.com/spotify/scio/releases).

### Upgrading Scio
If you're trying to upgrade Scio, start at the release for the new target version.
Look in the "Dependency Updates" section for a line like "Update Beam to X.YY.Z (#NNNN)".
You must bump the Beam dependency in `build.sbt` to the mentioned version at the same
time you bump Scio.

If you don't see a line with that pattern in the target Scio release, repeat the process
for each older release, until you hit the Scio version currently used by ingest-utils.
You must upgrade Beam to the latest-mentioned version.

### Upgrading Beam
If you're trying to ugprade Beam, you'll need to wait for a compatible Scio release.
Starting from the page for the version after what's currently used by ingest-utils,
look for a line in the "Dependency Updates" section with the pattern "Update Beam to X.YY.Z (#NNNN)".
You must bump the Scio dependency in `build.sbt` to at least the version that mentions
your target Beam revision. It's possible that there might be a later Scio release which
targets the same Beam version; these should be safe to use.

Be careful if you've allowed ingest-utils to fall behind the Beam/Scio release cycles, though,
because it's possible that the latest Scio version might target a newer Beam release than you'd
like to use. In this case, you need to bisect the Scio release-notes to find the latest available
release compatible with the targeted Beam version.
