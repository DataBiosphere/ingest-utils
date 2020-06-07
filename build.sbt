lazy val `ingest-utilities` = project
  .in(file("."))
  .settings(publish / skip := true)
  .aggregate(`core-sbt-plugins`)

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

/** Plugins for Monster sbt projects that generate data for Jade datasets. */
/*lazy val `sbt-plugins-jade` = project
  .in(file("plugins/jade"))
  .enablePlugins(MonsterLibraryPlugin)
  .dependsOn(`sbt-plugins-core`)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.beachape" %% "enumeratum" % "1.5.15",
      "com.beachape" %% "enumeratum-circe" % "1.5.23",
      "io.circe" %% "circe-core" % "0.13.0",
      "io.circe" %% "circe-parser" % "0.13.0",
      "io.circe" %% "circe-derivation" % "0.13.0-M4",
      "org.scalatest" %% "scalatest" % "3.1.1" % Test
    )
  )

/** Plugins for Monster sbt projects that contain Scio processing pipelines. */
lazy val `sbt-plugins-scio` = project
  .in(file("plugins/scio"))
  .enablePlugins(MonsterLibraryPlugin)
  .dependsOn(`sbt-plugins-core`)
  .settings(commonSettings)

/** Plugins for Monster sbt projects that contain Helm charts. */
lazy val `sbt-plugins-helm` = project
  .in(file("plugins/helm"))
  .enablePlugins(MonsterLibraryPlugin)
  .dependsOn(`sbt-plugins-core`)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-yaml" % "0.12.0",
      "org.scalatest" %% "scalatest" % "3.1.1" % Test
    )
  )
*/
