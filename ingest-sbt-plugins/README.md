# Ingest Plugins
This project contains `sbt` plugins which define common settings and build tasks used to
build, test, and publish ETL pipelines.

## Jade Dataset Plugin
The `MonsterJadeDatasetPlugin` bundled in this project enables useful functionality
for writing and interacting with BigQuery datasets managed by the Terra Data Repository.
Specifically, it:
1. Specifies a source tree for [defining Jade tables](#table-specification-syntax)
   using JSON files
2. Configures tasks to generate files from Jade table definitions, including:
   * Scala case classes, with JSON serializers
   * Jade-API-compatible dataset definition files
   * BigQuery-API-compatible schema definition files
3. Enables the `MonsterDockerPlugin`, overriding its default behavior to build
   [images](#schema-images) based on the `gcloud` SDK, with generated BigQuery
   metadata bundled into conventional locations

### Table Specification Syntax
We use a custom JSON syntax to specify Jade tables. This custom syntax largely
matches Jade's native API, but differs when needed to:
1. Add extra information needed to generate useful Scala code
2. Remove options that we see as bad practice
3. Reduce the amount of boilerplate we need to keep track of in our heads

Every table is specified as a JSON object, with one table per file. The only
required property of a table is the `name`, which must have a valid Jade identifier
as its value. Tables can also contain:
1. A `columns` property containing an array of ["simple"](#simple-columns) column specs.
2. A `struct_columns` property containing an array of ["struct"](#struct-columns) column specs.

#### Simple Columns
"Simple" columns cover every data types and mode except for structs. Each specification
is a single JSON object. The two required properties are:
1. `name`: A valid Jade identifier to use as the column name
2. `datatype`: The [type of the values](#column-datatypes) that will be stored within the column
In addition, simple columns can specify:
1. `type`: The [type / cardinality of the column itself](#column-types), defaulting to "optional"
2. `links`: An array of [foreign key reference](#column-links) to other columns in the same dataset

##### Column Datatypes
We allow the use of a limited subset of Jade datatypes within our schemas. Every type
is mapped to a Scala or raw BigQuery equivalent during code generation.

| Monster/Jade Type | Scala Type | BigQuery Type |
| ----------------- | ---------- | ------------- |
| boolean | `scala.Boolean` | BOOL |
| float | `scala.Double` | FLOAT64 |
| integer | `scala.Long` | INT64 |
| string | `java.lang.String` | STRING |
| date | `java.time.LocalDate` | DATE |
| timestamp | `java.time.OffsetDateTime` | TIMESTAMP |
| dir_ref | `java.lang.String` | STRING |
| file_ref | `java.lang.String` | STRING |

##### Column Types
Our column type specification is entirely custom. Every option maps to a Scala or
raw BigQuery equivalent during code generation. All but one map to an equivalent
representation in Jade dataset generation.
| Monster Type | Scala Type for Datatype `X` | BigQuery Mode | Jade Translation |
| ------------ | ---------- | ------------- | ---------------- |
| primary_key | `X` | REQUIRED | Column added to `primaryKey` array of enclosing table |
| required | `X` | REQUIRED | None |
| optional | `scala.Option[X]` | NULLABLE | "Normal" column |
| repeated | `scala.collection.immutable.List[X]` | REPEATED | `array_of` set to true for column |

##### Column Links
Our column link specification replaces Jade's built-in relationship construct.
Only one "side" of each relationship should specify a link; by convention, we usually
pick the foreign key side, pointing back at a primary key.

Links are specified as JSON objects with two required properties:
1. `table_name`: Name of the Jade table the link points to, potentially the table
   containing the link
2. `column_name`: Name of the column in `table_name` the link points to

At dataset generation time, all links are translated to Jade relationships. Random UUIDs
are used for the relationship names.

#### Struct Columns
Like simple columns, struct columns:
1. Are specified as JSON objects
2. Require a `name` property
3. Support an optional `type` property
Unlike simple columns, they:
1. Do not allow setting a `datatype` or `links`
2. Require a `struct_name` property pointing to the name of a
   [struct](#struct-specification-syntax) defined in the dataset

During Scala codegen, struct columns will be assigned the fully-qualified type
of the generated case class that matches `struct_name`. Codegen won't complain
if no such struct exists, but the generated code won't compile.

### Struct Specification Syntax
Although BigQuery supports structs, Jade does not, so the structs specified in our
dataset schemas are a custom invention. Like tables, structs are specified as JSON
objects, with one struct per file. Structs require two properties:
1. `name`: A valid Jade identifier. Any struct columns in tables that want to link
   against this struct must store the `name` in their `struct_name` properties
2. `fields`: An array of simple column definitions

The simple columns in a struct can specify all of the properties supported for simple
columns in tables, but:
1. `type` will only affect Scala code generation
2. `links` will be completely ignored

Struct models are generated into Scala case classes using mostly the same logic
as tables. The most significant difference is that the JSON encoders generated for
structs include a post-processing step to string-encode the JSON output, allowing
for structs to be stored in BigQuery STRING columns.

### Schema Images
Projects that enable the `MonsterJadeDatasetPlugin` will have their `publish` task
rewired to:
1. Use the table JSON definitions in the project to generate BigQuery metadata files
2. Bundle all generated metadata files into a Docker image based on the `gcloud` SDK
3. Push the bundled Docker image into GCR

Each table will generate three metadata files:
1. `schema.json`: A BigQuery [JSON schema file](https://cloud.google.com/bigquery/docs/schemas#specifying_a_json_schema_file)
   that matches the names, datatypes, and types of the table's columns
2. `primary-keys`: A text file containing the comma-separated names of all primary-key
   columns in the table
3. `compare-cols`: A text file containing the comma-separated names of all non-primary-key
   columns in the table which should be compared to detect differences on repeated ingest

For some table named `<tbl>`, the three metadata files will be written to the
`/bq-metadata/<tbl>/` directory within the published Docker image. The files can be
used to generate BigQuery SQL statements during ingest workflows.

## Scio Plugin
The `MonsterScioPipelinePlugin` bundled in this project enables useful functionality
for writing transformation pipelines using [Scio](https://github.com/spotify/scio).
Specifically, it:
1. Adds a dependency on Monster's [library](https://github.com/broadinstitute/monster-scio-utils/) of Scio utilities
2. Enables Scio-specific compiler flags
3. Configures the `MonsterDockerPlugin`, so that `publish` produces Argo-friendly
   pipeline runners

This plugin should be enabled on every Monster project containing a Scio pipeline
that runs as part of data ingest.

## Helm Plugin
The `MonsterHelmPlugin` bundled in this project provides tasks for packaging,
uploading, and indexing Helm charts to repositories hosted by GitHub pages.
Most of its business logic is delegated to `helm` and `chart-releaser`, so if
you're working on a "pure" Helm chart you probably just want to use those tools.

If you're working on a Helm chart that calls other tools from within the same project,
this chart can provide value by:
1. Wiring the `publish` task to also publish the Helm chart, along with any JARs / containers
2. Adding some pre-processing logic to inject the git-based version of the whole
   project into chart metadata before publishing
3. Wiring the `test` tasks to ensure that the output of `helm template` produces valid
   YAML when run with example inputs

### Using the Plugin
The plugin's existing tasks are intended to be used from a GitHub action (though
they should also work fine locally). You'll need to configure git and install tools
for the `publish` task to work correctly. The minimal set of required `steps` is:
```yaml
steps:
  - uses: actions/checkout@v2
  # Required to check out the `gh-pages` branch.
  - name: Fetch full git history
    run: git fetch --prune --unshallow
  # Required to push updates to `gh-pages`.
  - name: Configure git
    run: |
      git config user.name "$GITHUB_ACTOR"
      git config user.email "$GITHUB_ACTOR@users.noreply.github.com"
  # Required for sbt to work
  - uses: olafurpg/setup-scala@v7
    with:
      java-version: graalvm@20.0.0
  # Install Helm and chart-releaser
  - uses: azure/setup-helm@v1
    with:
      version: 'latest'
  - uses: broadinstitute/setup-chart-releaser@v1
    with:
      version: 'latest'
  # Upload the chart, and reindex the repository
  - name: Publish
    run: sbt publish reindexHelmRepository
    env:
      CR_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Testing Charts
Writing Helm charts can be a very frustrating task at scale. To help with the lowest
bar of error-detection, the `MonsterHelmPlugin` overwrites the `test` task to check:
1. The chart's templating logic is correct (no bad value references, no invalid functions)
2. The output of templating is valid YAML

To perform these checks, the plugin requires users to define example inputs for the
chart. By default, these inputs should be placed in an `example-values` directory
within the chart. The inputs should be defined in YAML, as if they were a `values.yaml`
file passed when calling `helm install`.
