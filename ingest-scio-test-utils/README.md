# Scio Test Utilities
This project defines common helper methods used to test
(or integration-test) ETL pipelines written using Scio.

## Using the library
This library is published to Broad's Artifactory instance. Any project
which enables the `MonsterBasePlugin` should be configured to pull dependencies
from that repository using:
```sbt
libraryDependencies += "org.broadinstitute.monster" %% "ingest-scio-test-utils" % "<version>" % Test
```
