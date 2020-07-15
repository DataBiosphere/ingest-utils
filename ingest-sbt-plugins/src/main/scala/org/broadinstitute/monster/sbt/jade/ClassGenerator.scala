package org.broadinstitute.monster.sbt.jade

import java.nio.file.{Files, Path}

import io.circe.Decoder
import io.circe.jawn.JawnParser
import org.broadinstitute.monster.sbt.jade.model._
import sbt._
import sbt.internal.util.ManagedLogger
import sbt.nio.file.{FileAttributes, FileTreeView}

/** Utilities for generating Scala classes from Monster table definitions. */
object ClassGenerator {
  private val parser = new JawnParser()

  /** Scala keywords which must be escaped if used as a column / table name. */
  private val keywords = Set("type")

  /**
    * Convert the contents of a local (non-Scala) file into Scala source code.
    *
    * The actual class-generation logic is passed into this method as an argument.
    * This method is useful because it provides common caching logic using sbt's
    * build-in file watching capabilities, re-generating classes only when their
    * source files change. Most of the logic here is copy-pasted from docs:
    * https://www.scala-sbt.org/1.x/docs/Howto-Track-File-Inputs-and-Outputs.html#File+inputs
    *
    * @param inputFiles     list of files on disk which should be converted into Scala
    *                       source files
    * @param inputChanges   description of filesystem changes since the last time
    *                       the task was triggered
    * @param inputExtension common file extension for each input file
    * @param outputDir      directory where generated Scala classes should be written
    * @param fileView       utility which can inspect the local filesystem
    * @param logger         utility which can write logs to the sbt console
    * @param gen            function which can convert input file contents to Scala source code
    */
  def generateClasses(
    inputFiles: Seq[Path],
    inputChanges: FileChanges,
    inputExtension: String,
    outputDir: Path,
    fileView: FileTreeView[(Path, FileAttributes)],
    logger: ManagedLogger,
    gen: String => Either[Throwable, String]
  ): Seq[File] = {
    def outputPath(path: Path): Path =
      outputDir / path.getFileName.toString.replaceAll(s".$inputExtension$$", ".scala")

    def generate(path: Path): Path = {
      val input = new String(Files.readAllBytes(path))
      val output = outputPath(path)
      logger.info(s"Generating $output from $path")
      gen(input) match {
        case Left(err) =>
          logger.trace(err)
          sys.error(s"Failed to generate $output from $path")
        case Right(codeString) =>
          Files.createDirectories(output.getParent)
          Files.write(output, codeString.getBytes())
      }
      output
    }

    // Build a mapping from expected-output-path -> source-input-path.
    val sourceMap = inputFiles.view.map(p => outputPath(p) -> p).toMap

    // Delete any files that exist in the output directory but don't
    // have a corresponding source file.
    val existingTargets = fileView
      .list(outputDir.toGlob / **)
      .flatMap {
        case (p, _) =>
          if (!sourceMap.contains(p)) {
            Files.deleteIfExists(p)
            None
          } else {
            Some(p)
          }
      }
      .toSet

    // Re-generate classes for new and updated source files.
    val toGenerate = (inputChanges.created ++ inputChanges.modified).toSet ++ sourceMap
      .filterKeys(!existingTargets.contains(_))
      .values
    toGenerate.foreach(generate)
    sourceMap.keys.toVector.map(_.toFile)
  }

  /**
    * Generate a Scala case class corresponding to a Jade table.
    *
    * @param targetPackage   package which should contain the class
    * @param fragmentPackage package where any fragments referenced by the table
    *                        should be located
    * @param structPackage   package where any structs referenced by the table
    *                        should be located
    * @param tableContent    JSON content of the Jade table file
    */
  def generateTableClass[T <: ClassSpec: Decoder](
    targetPackage: String,
    fragmentPackage: String,
    structPackage: String,
    tableContent: String
  ): Either[Throwable, String] =
    parser.decode[T](tableContent).map { baseTable =>
      // Get the parameters for the class definition
      val name = snakeToCamel(baseTable.name, titleCase = true)
      val simpleFields = baseTable.columns.map(fieldForColumn)
      val structFields = baseTable.structColumns.map(fieldForStruct(structPackage, _))
      val composedFields = baseTable.tableFragments.map(fieldForComposedTable(fragmentPackage, _))
      val classParams = (simpleFields ++ structFields ++ composedFields)
        .map(f => s"\n$f")
        .mkString(",")

      // Get the parameters for the init method definition
      val requiredColumnParams = baseTable.columns.filter(_.`type`.isRequired).map(fieldForColumn)
      val requiredStructParams = baseTable.structColumns
        .filter(_.`type`.isRequired)
        .map(fieldForStruct(structPackage, _))
      val requiredParams = (requiredColumnParams ++ requiredStructParams)
        .map(f => s"\n    $f")
        .mkString(",")

      // Get the values that should be used to initialize the object
      val simpleInitFields = baseTable.columns.map(initValueForColumn)
      val structInitFields = baseTable.structColumns.map(initValueForStruct(structPackage, _))
      val composedInitFields =
        baseTable.tableFragments.map(initValueForComposedTable(fragmentPackage, _))
      val initClassParams = (simpleInitFields ++ structInitFields ++ composedInitFields)
        .map(f => s"\n      $f")
        .mkString(",")
      val encoder = generateEncoder(
        baseTable.name,
        baseTable.columns,
        baseTable.structColumns,
        baseTable.tableFragments,
        structPackage,
        Some(fragmentPackage)
      )
      val composedKeys = baseTable.tableFragments.map(_.id).map(k => s""""$k"""")
      val composedKeysDeclaration = if (composedKeys.nonEmpty) {
        s"""
           |  val composedKeys: _root_.scala.collection.immutable.Set[_root_.java.lang.String] =
           |    _root_.scala.collection.immutable.Set(${composedKeys.mkString(", ")})
           |"""
      } else ""

      s"""package $targetPackage
         |
         |case class $name($classParams)
         |
         |object $name {$composedKeysDeclaration
         |  $encoder
         |
         |  def init($requiredParams): $name = {
         |    $name($initClassParams)
         |  }
         |}
         |""".stripMargin
    }

  /**
    * Generate custom encoder that maps the field names of a scala class
    * to their corresponding schema column names.
    *
    * @param className the snake-case name of the Jade table or struct for which the encoder is being constructed
    * @param simpleColumns the set of columns without nested fields on the Jade table
    * @param structColumns the set of columns with nested fields in the Jade table
    * @param tableFragments the set of partial table definitions associated with the Jade table
    * @param structPackage  package where any structs referenced by the table should be located
    * @param fragmentPackage package where any fragments referenced by the table should be located
    * @param isStructClass true only if the encoder is being generated for a struct
    */
  def generateEncoder(
    className: JadeIdentifier,
    simpleColumns: Vector[SimpleColumn],
    structColumns: Vector[StructColumn],
    tableFragments: Vector[JadeIdentifier],
    structPackage: String,
    fragmentPackage: Option[String] = None,
    isStructClass: Boolean = false
  ): String = {
    val camelCaseName = snakeToCamel(className, titleCase = false)
    val titleCaseName = snakeToCamel(className, titleCase = true)
    val encoderDeclaration = s"implicit val encoder: _root_.io.circe.Encoder[${titleCaseName}]"
    val encoderMapping = generateEncoderMapping(
      className,
      simpleColumns,
      structColumns,
      tableFragments,
      structPackage,
      fragmentPackage
    )

    if (isStructClass) {
      s"""$encoderDeclaration = { $camelCaseName =>
         |    val jsonObj = _root_.io.circe.Json.obj($encoderMapping
         |    )
         |    _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
         |  }""".stripMargin
    } else if (tableFragments.nonEmpty) { // TODO use map instead of json obj
      s"""$encoderDeclaration = { $camelCaseName =>
         |    val jsonObj = _root_.io.circe.JsonObject($encoderMapping
         |    )
         |    val composed = jsonObj.filterKeys(composedKeys.contains(_))
         |    val notComposed = jsonObj.filterKeys(!composedKeys.contains(_))
         |
         |    _root_.io.circe.Json.fromJsonObject(composed.toIterable.foldLeft(notComposed) {
         |      case (acc, (_, subTable)) => acc.deepMerge(subTable.asObject.get)
         |    })
         |  }""".stripMargin
    } else {
      s"""$encoderDeclaration =
         |    $camelCaseName => _root_.io.circe.Json.obj($encoderMapping
         |    )""".stripMargin
    }
  }

  /**
    * Generate a mapping for a custom encoder that maps the field names of a scala class
    * to their corresponding schema column names.
    *
    * @param className the snake-case name of the Jade table or struct for which the encoder is being constructed
    * @param simpleColumns the set of columns without nested fields on the Jade table
    * @param structColumns the set of columns with nested fields in the Jade table
    * @param tableFragments the set of partial table definitions associated with the Jade table
    * @param structPackage package where any structs referenced by the table should be located
    * @param fragmentPackage package where any fragments referenced by the table should be located
    */
  def generateEncoderMapping(
    className: JadeIdentifier,
    simpleColumns: Vector[SimpleColumn],
    structColumns: Vector[StructColumn],
    tableFragments: Vector[JadeIdentifier],
    structPackage: String,
    fragmentPackage: Option[String] = None
  ): String = {
    val camelCaseName = snakeToCamel(className, titleCase = false)
    val columnEncoderMappings = simpleColumns.map { column =>
      val columnType = column.`type`.modify(column.datatype.asScala)
      generateEncoderMappingLine(column.name.id, columnType, camelCaseName, getFieldName(column.name))
    }
    val structEncoderMappings = structColumns.map { column =>
      val columnType = column.`type`.modify(getStructType(structPackage, column))
      generateEncoderMappingLine(column.name.id, columnType, camelCaseName, getFieldName(column.name))
    }
    val fragmentEncoderMappings = tableFragments.flatMap { fragment =>
      for (fragPackage <- fragmentPackage) yield {
        val columnType = ColumnType.Optional.modify(composedTableType(fragPackage, fragment))
        generateEncoderMappingLine(fragment.id, columnType, camelCaseName, getFieldName(fragment))
      }
    }
    (columnEncoderMappings ++ structEncoderMappings ++ fragmentEncoderMappings).mkString(",")
  }

  /**
    * Generate a single line of code to be used in a custom encoder for a scala class.
    *
    * @param columnName the snake-case name of the field/column
    * @param dataType the scala datatype of the field/column for which the mapping is being generated
    * @param className the snake-case name of the Jade table or struct for which the encoder is being generated
    * @param fieldName the name of the field for which the mapping is being generated
    */
  def generateEncoderMappingLine(
    columnName: String,
    dataType: String,
    className: String,
    fieldName: String
  ): String = {
    s"""
       |      "$columnName" -> _root_.io.circe.Encoder[$dataType].apply($className.$fieldName)""".stripMargin
  }

  /**
    * Generate a Scala case class corresponding to a nested struct.
    *
    * @param structPackage package which should contain the class
    * @param structContent JSON content of the Jade struct file
    */
  def generateStructClass(
    structPackage: String,
    structContent: String
  ): Either[Throwable, String] =
    parser.decode[Struct](structContent).map { baseStruct =>
      val name = snakeToCamel(baseStruct.name, titleCase = true)
      val fields = baseStruct.fields.map(fieldForColumn)
      val classParams = fields.map(f => s"\n$f").mkString(",")
      val encoder = generateEncoder(
        baseStruct.name,
        baseStruct.fields,
        Vector.empty,
        Vector.empty,
        structPackage,
        isStructClass = true
      )

      s"""package $structPackage
         |
         |case class $name($classParams)
         |
         |object $name {
         |  $encoder
         |}
         |""".stripMargin
    }

  /** Get the Scala field name for a Jade column. */
  private def getFieldName(columnName: JadeIdentifier): String = {
    val rawColumnName = snakeToCamel(columnName, titleCase = false)
    if (keywords.contains(rawColumnName)) s"`$rawColumnName`" else rawColumnName
  }

  /** Get the data type for a column which references a struct. */
  private def getStructType(structPackage: String, structColumn: StructColumn): String =
    s"_root_.$structPackage.${snakeToCamel(structColumn.structName, titleCase = true)}"

  /**
    * Set the field name equal to the field's default value.
    * If there is no default value, set it equal to the its own field name.
    */
  private def initValueForColumn(jadeColumn: SimpleColumn): String = {
    val columnName = getFieldName(jadeColumn.name)
    val defaultValue = jadeColumn.`type`.getDefaultValue(jadeColumn.datatype.asScala)
    s"$columnName = ${defaultValue.getOrElse(columnName)}"
  }

  /**
    * Set the field name equal to the field's default value.
    * If there is no default value, set it equal to the its own field name.
    */
  private def initValueForStruct(structPackage: String, structColumn: StructColumn): String = {
    val columnName = getFieldName(structColumn.name)
    val structType = getStructType(structPackage, structColumn)
    val defaultValue = structColumn.`type`.getDefaultValue(structType)
    s"$columnName = ${defaultValue.getOrElse(columnName)}"
  }

  /** Get the Scala field declaration for a Jade column. */
  private def fieldForColumn(jadeColumn: SimpleColumn): String = {
    val modifiedType = jadeColumn.`type`.modify(jadeColumn.datatype.asScala)
    s"${getFieldName(jadeColumn.name)}: $modifiedType"
  }

  /** Get the Scala field declaration for a Jade column which references a struct. */
  private def fieldForStruct(structPackage: String, structColumn: StructColumn): String = {
    val structType = getStructType(structPackage, structColumn)
    val modifiedType = structColumn.`type`.modify(structType)
    s"${getFieldName(structColumn.name)}: $modifiedType"
  }

  private def composedTableType(tablePackage: String, tableName: JadeIdentifier): String =
    s"_root_.$tablePackage.${snakeToCamel(tableName, titleCase = true)}"

  /** Get the Scala field declaration for a composed Jade table. */
  private def fieldForComposedTable(tablePackage: String, tableName: JadeIdentifier): String = {
    val tableType = composedTableType(tablePackage, tableName)
    val modifiedType = ColumnType.Optional.modify(tableType)
    s"${getFieldName(tableName)}: $modifiedType"
  }

  /** Get a parameter definition for a composed Jade table. */
  private def initValueForComposedTable(tablePackage: String, tableName: JadeIdentifier): String = {
    val tableType = composedTableType(tablePackage, tableName)
    val defaultValue = ColumnType.Optional.getDefaultValue(tableType).get
    s"${getFieldName(tableName)} = $defaultValue"
  }

  /**
    * Convert a snake_case Jade identifier to Scala camelCase.
    *
    * Jade IDs are enforced to be snake_case on deserialization.
    *
    * @param titleCase if true, the first character of the output will be capitalized
    */
  private def snakeToCamel(id: JadeIdentifier, titleCase: Boolean): String = {
    val res = id.id.split("_", 0).map(x => x(0).toUpper + x.drop(1)).mkString
    if (titleCase) {
      res
    } else {
      res(0).toLower + res.drop(1)
    }
  }
}
