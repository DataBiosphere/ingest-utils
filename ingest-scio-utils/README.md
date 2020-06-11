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
