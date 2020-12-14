lazy val `ingest-utilities` = project
  .in(file("."))
  .settings(publish / skip := true)
  .aggregate(`core-sbt-plugins`, `ingest-sbt-plugins`, `ingest-scio-utils`, `ingest-scio-test-utils`)

/** 'Core' plugins for use across all Monster sbt projects. */
lazy val `core-sbt-plugins` = project
  .in(file("core-sbt-plugins"))
  .enablePlugins(MonsterLibraryPlugin)
  .settings(
    sbtPlugin := true,
    addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0"),
    addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0"),
    addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.3"),
    addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0"),
    addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
  )

/** Plugins for bootstrapping ingest ETL projects. */
lazy val `ingest-sbt-plugins` = project
  .in(file("ingest-sbt-plugins"))
  .enablePlugins(MonsterLibraryPlugin)
  .dependsOn(`core-sbt-plugins`)
  .settings(
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      "com.beachape" %% "enumeratum" % "1.5.15",
      "com.beachape" %% "enumeratum-circe" % "1.5.23",
      "io.circe" %% "circe-core" % "0.13.0",
      "io.circe" %% "circe-parser" % "0.13.0",
      "io.circe" %% "circe-derivation" % "0.13.0-M4",
      "io.circe" %% "circe-yaml" % "0.13.1",
      "org.scalatest" %% "scalatest" % "3.1.1" % Test
    ),
    buildInfoKeys ++= Seq(
      BuildInfoKey("scioUtilsName", (`ingest-scio-utils` / name).value),
      BuildInfoKey("scioTestUtilsName", (`ingest-scio-test-utils` / name).value)
    )
  )

// NOTE: These constants aren't used above because the two lists of dependencies
// operate at different "levels". Everything above turns into a dependency of the
// build itself, while everything below is going to be injected as app-level libraries.
// There's no inherent need to keep the two in-sync, and if we ever want to port to
// Scala 2.13 before sbt catches up it's very likely the two lists will drift.
val beamVersion = "2.24.0"
val betterFilesVersion = "3.8.0"
val circeVersion = "0.13.0"
val circeDerivationVersion = "0.13.0-M4"
val logbackVersion = "1.2.3"
val scioVersion = "0.9.5"
val uPickleVersion = "1.0.0"

val scalatestVersion = "3.1.1"

/** Library containing common ETL logic for batch Scio pipelines. */
lazy val `ingest-scio-utils` = project
  .in(file("ingest-scio-utils"))
  .enablePlugins(MonsterLibraryPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.lihaoyi" %% "upickle" % uPickleVersion,
      "com.spotify" %% "scio-core" % scioVersion,
      "io.circe" %% "circe-parser" % circeVersion
    ),
    libraryDependencies ++= Seq(
      "org.apache.beam" % "beam-runners-direct-java" % beamVersion,
      "org.apache.beam" % "beam-runners-google-cloud-dataflow-java" % beamVersion
    ).map(_ % Runtime),
    libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files" % betterFilesVersion,
      "com.spotify" %% "scio-test" % scioVersion,
      "io.circe" %% "circe-derivation" % circeDerivationVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion
    ).map(_ % Test),
    Test / scalacOptions += "-language:higherKinds"
  )

/** Library containing common utilities for testing batch Scio pipelines. */
lazy val `ingest-scio-test-utils` = project
  .in(file("ingest-scio-test-utils"))
  .enablePlugins(MonsterLibraryPlugin)
  .dependsOn(`ingest-scio-utils`)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files" % betterFilesVersion,
      "com.spotify" %% "scio-test" % scioVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion
    )
  )
