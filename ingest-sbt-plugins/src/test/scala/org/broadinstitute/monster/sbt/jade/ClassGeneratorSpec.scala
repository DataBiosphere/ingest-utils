package org.broadinstitute.monster.sbt

import org.broadinstitute.monster.sbt.jade.ClassGenerator
import org.broadinstitute.monster.sbt.jade.model.{MonsterTable, MonsterTableFragment}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ClassGeneratorSpec extends AnyFlatSpec with Matchers with EitherValues {
  behavior of "ClassGenerator"

  private val testPackage = "foo.bar"
  private val fragmentPackage = "xyz.abc"
  private val structPackage = testPackage.reverse

  def checkTableGeneration(description: String, input: String, output: String): Unit =
    it should description in {
      val out = ClassGenerator
        .generateTableClass[MonsterTable](testPackage, fragmentPackage, structPackage, input)
      out.right.value shouldBe output
    }

  def checkFailedTableGeneration(
    description: String,
    input: String,
    error: String
  ): Unit =
    it should description in {
      val out = ClassGenerator
        .generateTableClass[MonsterTable](testPackage, fragmentPackage, structPackage, input)
      out.left.value.getMessage should include(error)
    }

  def checkFragmentGeneration(description: String, input: String, output: String): Unit =
    it should description in {
      val out = ClassGenerator.generateTableClass[MonsterTableFragment](
        fragmentPackage,
        fragmentPackage,
        structPackage,
        input
      )
      out.right.value shouldBe output
    }

  def checkStructGeneration(description: String, input: String, output: String): Unit =
    it should description in {
      val out = ClassGenerator.generateStructClass(structPackage, input)
      out.right.value shouldBe output
    }

  def checkFailedStructGeneration(
    description: String,
    input: String,
    error: String
  ): Unit =
    it should description in {
      val out = ClassGenerator.generateStructClass(structPackage, input)
      out.left.value.getMessage should include(error)
    }

  // Happy table cases
  it should behave like checkTableGeneration(
    "generate a zero-column table",
    s"""{
       |  "name": "no_columns",
       |  "columns": []
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class NoColumns()
       |
       |object NoColumns {
       |  implicit val encoder: _root_.io.circe.Encoder[NoColumns] =
       |    noColumns => _root_.io.circe.Json.obj(
       |    )
       |
       |  def init(): NoColumns = {
       |    NoColumns()
       |  }
       |}
       |""".stripMargin
  )
  it should behave like checkTableGeneration(
    "generate a one-column table",
    s"""{
       |  "name": "one_column",
       |  "columns": [
       |    {
       |      "name": "test_column",
       |      "datatype": "string"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class OneColumn(
       |testColumn: _root_.scala.Option[_root_.java.lang.String])
       |
       |object OneColumn {
       |  implicit val encoder: _root_.io.circe.Encoder[OneColumn] =
       |    oneColumn => _root_.io.circe.Json.obj(
       |      "test_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(oneColumn.testColumn)
       |    )
       |
       |  def init(): OneColumn = {
       |    OneColumn(
       |      testColumn = _root_.scala.Option.empty[_root_.java.lang.String])
       |  }
       |}
       |""".stripMargin
  )
  it should behave like checkTableGeneration(
    "generate every type of column",
    s"""{
       |  "name": "all_columns",
       |  "columns": [
       |    {
       |      "name": "bool_column",
       |      "datatype": "boolean"
       |    },
       |    {
       |      "name": "float_column",
       |      "datatype": "float"
       |    },
       |    {
       |      "name": "int_column",
       |      "datatype": "integer"
       |    },
       |    {
       |      "name": "string_column",
       |      "datatype": "string"
       |    },
       |    {
       |      "name": "date_column",
       |      "datatype": "date"
       |    },
       |    {
       |      "name": "timestamp_column",
       |      "datatype": "timestamp"
       |    },
       |    {
       |      "name": "dir_column",
       |      "datatype": "dirref"
       |    },
       |    {
       |      "name": "file_column",
       |      "datatype": "fileref"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class AllColumns(
       |boolColumn: _root_.scala.Option[_root_.scala.Boolean],
       |floatColumn: _root_.scala.Option[_root_.scala.Double],
       |intColumn: _root_.scala.Option[_root_.scala.Long],
       |stringColumn: _root_.scala.Option[_root_.java.lang.String],
       |dateColumn: _root_.scala.Option[_root_.java.time.LocalDate],
       |timestampColumn: _root_.scala.Option[_root_.java.time.OffsetDateTime],
       |dirColumn: _root_.scala.Option[_root_.java.lang.String],
       |fileColumn: _root_.scala.Option[_root_.java.lang.String])
       |
       |object AllColumns {
       |  implicit val encoder: _root_.io.circe.Encoder[AllColumns] =
       |    allColumns => _root_.io.circe.Json.obj(
       |      "bool_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Boolean]].apply(allColumns.boolColumn),
       |      "float_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Double]].apply(allColumns.floatColumn),
       |      "int_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Long]].apply(allColumns.intColumn),
       |      "string_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allColumns.stringColumn),
       |      "date_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.time.LocalDate]].apply(allColumns.dateColumn),
       |      "timestamp_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.time.OffsetDateTime]].apply(allColumns.timestampColumn),
       |      "dir_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allColumns.dirColumn),
       |      "file_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allColumns.fileColumn)
       |    )
       |
       |  def init(): AllColumns = {
       |    AllColumns(
       |      boolColumn = _root_.scala.Option.empty[_root_.scala.Boolean],
       |      floatColumn = _root_.scala.Option.empty[_root_.scala.Double],
       |      intColumn = _root_.scala.Option.empty[_root_.scala.Long],
       |      stringColumn = _root_.scala.Option.empty[_root_.java.lang.String],
       |      dateColumn = _root_.scala.Option.empty[_root_.java.time.LocalDate],
       |      timestampColumn = _root_.scala.Option.empty[_root_.java.time.OffsetDateTime],
       |      dirColumn = _root_.scala.Option.empty[_root_.java.lang.String],
       |      fileColumn = _root_.scala.Option.empty[_root_.java.lang.String])
       |  }
       |}
       |""".stripMargin
  )
  // NOTE: This is copy-pasted from the expected output of the test above.
  // Scalatest's compilation-checking helpers only work on string literals (sigh),
  // so we can't run the check automatically as part of the existing helper function.
  it should "output compile-able code with all types" in {
    """case class AllColumns(
       boolColumn: _root_.scala.Option[_root_.scala.Boolean],
       floatColumn: _root_.scala.Option[_root_.scala.Double],
       intColumn: _root_.scala.Option[_root_.scala.Long],
       stringColumn: _root_.scala.Option[_root_.java.lang.String],
       dateColumn: _root_.scala.Option[_root_.java.time.LocalDate],
       timestampColumn: _root_.scala.Option[_root_.java.time.OffsetDateTime],
       dirColumn: _root_.scala.Option[_root_.java.lang.String],
       fileColumn: _root_.scala.Option[_root_.java.lang.String])

       object AllColumns {
        implicit val encoder: _root_.io.circe.Encoder[AllColumns] =
          allColumns => _root_.io.circe.Json.obj(
            "bool_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Boolean]].apply(allColumns.boolColumn),
            "float_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Double]].apply(allColumns.floatColumn),
            "int_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Long]].apply(allColumns.intColumn),
            "string_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allColumns.stringColumn),
            "date_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.time.LocalDate]].apply(allColumns.dateColumn),
            "timestamp_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.time.OffsetDateTime]].apply(allColumns.timestampColumn),
            "dir_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allColumns.dirColumn),
            "file_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allColumns.fileColumn)
          )

         def init(): AllColumns = {
           AllColumns(
             boolColumn = _root_.scala.Option.empty[_root_.scala.Boolean],
             floatColumn = _root_.scala.Option.empty[_root_.scala.Double],
             intColumn = _root_.scala.Option.empty[_root_.scala.Long],
             stringColumn = _root_.scala.Option.empty[_root_.java.lang.String],
             dateColumn = _root_.scala.Option.empty[_root_.java.time.LocalDate],
             timestampColumn = _root_.scala.Option.empty[_root_.java.time.OffsetDateTime],
             dirColumn = _root_.scala.Option.empty[_root_.java.lang.String],
             fileColumn = _root_.scala.Option.empty[_root_.java.lang.String])
         }
       }
       """ should compile
  }

  it should behave like checkTableGeneration(
    "generate required columns",
    s"""{
       |  "name": "required_column",
       |  "columns": [
       |    {
       |      "name": "test_required",
       |      "datatype": "string",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class RequiredColumn(
       |testRequired: _root_.java.lang.String)
       |
       |object RequiredColumn {
       |  implicit val encoder: _root_.io.circe.Encoder[RequiredColumn] =
       |    requiredColumn => _root_.io.circe.Json.obj(
       |      "test_required" -> _root_.io.circe.Encoder[_root_.java.lang.String].apply(requiredColumn.testRequired)
       |    )
       |
       |  def init(
       |    testRequired: _root_.java.lang.String): RequiredColumn = {
       |    RequiredColumn(
       |      testRequired = testRequired)
       |  }
       |}
       |""".stripMargin
  )
  it should behave like checkTableGeneration(
    "generate primary key columns",
    s"""{
       |  "name": "key_column",
       |  "columns": [
       |    {
       |      "name": "test_key",
       |      "datatype": "string",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class KeyColumn(
       |testKey: _root_.java.lang.String)
       |
       |object KeyColumn {
       |  implicit val encoder: _root_.io.circe.Encoder[KeyColumn] =
       |    keyColumn => _root_.io.circe.Json.obj(
       |      "test_key" -> _root_.io.circe.Encoder[_root_.java.lang.String].apply(keyColumn.testKey)
       |    )
       |
       |  def init(
       |    testKey: _root_.java.lang.String): KeyColumn = {
       |    KeyColumn(
       |      testKey = testKey)
       |  }
       |}
       |""".stripMargin
  )
  it should behave like checkTableGeneration(
    "generate array columns",
    s"""{
       |  "name": "array_column",
       |  "columns": [
       |    {
       |      "name": "test_array",
       |      "datatype": "float",
       |      "type": "repeated"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class ArrayColumn(
       |testArray: _root_.scala.collection.immutable.List[_root_.scala.Double])
       |
       |object ArrayColumn {
       |  implicit val encoder: _root_.io.circe.Encoder[ArrayColumn] =
       |    arrayColumn => _root_.io.circe.Json.obj(
       |      "test_array" -> _root_.io.circe.Encoder[_root_.scala.collection.immutable.List[_root_.scala.Double]].apply(arrayColumn.testArray)
       |    )
       |
       |  def init(): ArrayColumn = {
       |    ArrayColumn(
       |      testArray = _root_.scala.collection.immutable.List.empty[_root_.scala.Double])
       |  }
       |}
       |""".stripMargin
  )
  it should behave like checkTableGeneration(
    "mix all kinds of column types",
    s"""{
       |  "name": "all_modifiers",
       |  "columns": [
       |    {
       |      "name": "key_column",
       |      "datatype": "date",
       |      "type": "primary_key"
       |    },
       |    {
       |      "name": "normal_column",
       |      "datatype": "boolean"
       |    },
       |    {
       |      "name": "array_column",
       |      "datatype": "integer",
       |      "type": "repeated"
       |    },
       |    {
       |      "name": "required_column",
       |      "datatype": "float",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class AllModifiers(
       |keyColumn: _root_.java.time.LocalDate,
       |normalColumn: _root_.scala.Option[_root_.scala.Boolean],
       |arrayColumn: _root_.scala.collection.immutable.List[_root_.scala.Long],
       |requiredColumn: _root_.scala.Double)
       |
       |object AllModifiers {
       |  implicit val encoder: _root_.io.circe.Encoder[AllModifiers] =
       |    allModifiers => _root_.io.circe.Json.obj(
       |      "key_column" -> _root_.io.circe.Encoder[_root_.java.time.LocalDate].apply(allModifiers.keyColumn),
       |      "normal_column" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Boolean]].apply(allModifiers.normalColumn),
       |      "array_column" -> _root_.io.circe.Encoder[_root_.scala.collection.immutable.List[_root_.scala.Long]].apply(allModifiers.arrayColumn),
       |      "required_column" -> _root_.io.circe.Encoder[_root_.scala.Double].apply(allModifiers.requiredColumn)
       |    )
       |
       |  def init(
       |    keyColumn: _root_.java.time.LocalDate,
       |    requiredColumn: _root_.scala.Double): AllModifiers = {
       |    AllModifiers(
       |      keyColumn = keyColumn,
       |      normalColumn = _root_.scala.Option.empty[_root_.scala.Boolean],
       |      arrayColumn = _root_.scala.collection.immutable.List.empty[_root_.scala.Long],
       |      requiredColumn = requiredColumn)
       |  }
       |}
       |""".stripMargin
  )
  // NOTE: This is copy-pasted from the expected output of the test above.
  // Scalatest's compilation-checking helpers only work on string literals (sigh),
  // so we can't run the check automatically as part of the existing helper function.
  it should "output compile-able code with all modifiers" in {
    """case class AllModifiers(
       keyColumn: _root_.java.time.LocalDate,
       normalColumn: _root_.scala.Option[_root_.scala.Boolean],
       arrayColumn: _root_.scala.collection.immutable.List[_root_.scala.Long])
       """ should compile
  }

  it should behave like checkTableGeneration(
    "escape columns named `type` in generated code",
    s"""{
       |  "name": "type_column",
       |  "columns": [
       |    {
       |      "name": "type",
       |      "datatype": "float"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class TypeColumn(
       |`type`: _root_.scala.Option[_root_.scala.Double])
       |
       |object TypeColumn {
       |  implicit val encoder: _root_.io.circe.Encoder[TypeColumn] =
       |    typeColumn => _root_.io.circe.Json.obj(
       |      "type" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Double]].apply(typeColumn.`type`)
       |    )
       |
       |  def init(): TypeColumn = {
       |    TypeColumn(
       |      `type` = _root_.scala.Option.empty[_root_.scala.Double])
       |  }
       |}
       |""".stripMargin
  )
  it should "output compile-able code with escaped fields" in {
    """case class TypeColumn(
       `type`: _root_.scala.Option[_root_.scala.Double])
       """ should compile
  }

  it should behave like checkTableGeneration(
    "generate references to struct classes",
    s"""{
       |  "name": "struct_column",
       |  "struct_columns": [
       |    {
       |      "name": "comment",
       |      "struct_name": "row_comment"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class StructColumn(
       |comment: _root_.scala.Option[_root_.$structPackage.RowComment])
       |
       |object StructColumn {
       |  implicit val encoder: _root_.io.circe.Encoder[StructColumn] =
       |    structColumn => _root_.io.circe.Json.obj(
       |      "comment" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.$structPackage.RowComment]].apply(structColumn.comment)
       |    )
       |
       |  def init(): StructColumn = {
       |    StructColumn(
       |      comment = _root_.scala.Option.empty[_root_.$structPackage.RowComment])
       |  }
       |}
       |""".stripMargin
  )

  it should behave like checkTableGeneration(
    "generate required struct references",
    s"""{
       |  "name": "required_struct",
       |  "struct_columns": [
       |    {
       |      "name": "required_comment",
       |      "struct_name": "comment",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class RequiredStruct(
       |requiredComment: _root_.$structPackage.Comment)
       |
       |object RequiredStruct {
       |  implicit val encoder: _root_.io.circe.Encoder[RequiredStruct] =
       |    requiredStruct => _root_.io.circe.Json.obj(
       |      "required_comment" -> _root_.io.circe.Encoder[_root_.$structPackage.Comment].apply(requiredStruct.requiredComment)
       |    )
       |
       |  def init(
       |    requiredComment: _root_.$structPackage.Comment): RequiredStruct = {
       |    RequiredStruct(
       |      requiredComment = requiredComment)
       |  }
       |}
       |""".stripMargin
  )

  it should behave like checkTableGeneration(
    "generate repeated struct references",
    s"""{
       |  "name": "repeated_struct",
       |  "struct_columns": [
       |    {
       |      "name": "repeated_comment",
       |      "struct_name": "comment123",
       |      "type": "repeated"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class RepeatedStruct(
       |repeatedComment: _root_.scala.collection.immutable.List[_root_.$structPackage.Comment123])
       |
       |object RepeatedStruct {
       |  implicit val encoder: _root_.io.circe.Encoder[RepeatedStruct] =
       |    repeatedStruct => _root_.io.circe.Json.obj(
       |      "repeated_comment" -> _root_.io.circe.Encoder[_root_.scala.collection.immutable.List[_root_.$structPackage.Comment123]].apply(repeatedStruct.repeatedComment)
       |    )
       |
       |  def init(): RepeatedStruct = {
       |    RepeatedStruct(
       |      repeatedComment = _root_.scala.collection.immutable.List.empty[_root_.$structPackage.Comment123])
       |  }
       |}
       |""".stripMargin
  )

  it should behave like checkTableGeneration(
    "generate table-fragment references",
    s"""{
       |  "name": "composed_table",
       |  "columns": [
       |    {
       |      "name": "id",
       |      "datatype": "integer",
       |      "type": "primary_key"
       |    }
       |  ],
       |  "table_fragments": ["other_table", "third_table"]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class ComposedTable(
       |id: _root_.scala.Long,
       |otherTable: _root_.scala.Option[_root_.$fragmentPackage.OtherTable],
       |thirdTable: _root_.scala.Option[_root_.$fragmentPackage.ThirdTable])
       |
       |object ComposedTable {
       |  val composedKeys: _root_.scala.collection.immutable.Set[_root_.java.lang.String] =
       |    _root_.scala.collection.immutable.Set("other_table", "third_table")
       |
       |  implicit val encoder: _root_.io.circe.Encoder[ComposedTable] = { composedTable =>
       |    val jsonObj = _root_.io.circe.JsonObject(
       |      "id" -> _root_.io.circe.Encoder[_root_.scala.Long].apply(composedTable.id),
       |      "other_table" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.$fragmentPackage.OtherTable]].apply(composedTable.otherTable),
       |      "third_table" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.$fragmentPackage.ThirdTable]].apply(composedTable.thirdTable)
       |    )
       |    val composed = jsonObj.filterKeys(composedKeys.contains(_))
       |    val notComposed = jsonObj.filterKeys(!composedKeys.contains(_))
       |
       |    _root_.io.circe.Json.fromJsonObject(composed.toIterable.foldLeft(notComposed) {
       |      case (acc, (_, subTable)) => acc.deepMerge(subTable.asObject.get)
       |    })
       |  }
       |
       |  def init(
       |    id: _root_.scala.Long): ComposedTable = {
       |    ComposedTable(
       |      id = id,
       |      otherTable = _root_.scala.Option.empty[_root_.$fragmentPackage.OtherTable],
       |      thirdTable = _root_.scala.Option.empty[_root_.$fragmentPackage.ThirdTable])
       |  }
       |}
       |""".stripMargin
  )

  it should "be able to compile something similar to composed-fragments table encoder" in {
    """case class ComposedTable(
      id: _root_.scala.Long,
      otherTable: _root_.scala.Option[_root_.scala.Long],
      thirdTable: _root_.scala.Option[_root_.scala.Long])

      object ComposedTable {
        val composedKeys: _root_.scala.collection.immutable.Set[_root_.java.lang.String] =
          _root_.scala.collection.immutable.Set("other_table", "third_table")

        implicit val encoder: _root_.io.circe.Encoder[ComposedTable] = { composedTable =>
          val jsonObj = _root_.io.circe.JsonObject(
            "id" -> _root_.io.circe.Encoder[_root_.scala.Long].apply(composedTable.id),
            "other_table" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Long]].apply(composedTable.otherTable),
            "third_table" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Long]].apply(composedTable.thirdTable)
          )
          val composed = jsonObj.filterKeys(composedKeys.contains(_))
          val notComposed = jsonObj.filterKeys(!composedKeys.contains(_))

          _root_.io.circe.Json.fromJsonObject(composed.toIterable.foldLeft(notComposed) {
            case (acc, (_, subTable)) => acc.deepMerge(subTable.asObject.get)
          })
        }

        def init(
          id: _root_.scala.Long): ComposedTable = {
          ComposedTable(
            id = id,
            otherTable = _root_.scala.Option.empty[_root_.scala.Long],
            thirdTable = _root_.scala.Option.empty[_root_.scala.Long])
        }
      }""" should compile
  }

  it should behave like checkFragmentGeneration(
    "generate table-fragment classes",
    s"""{
       |  "name": "fragment_class",
       |  "columns": [
       |    {
       |      "name": "id",
       |      "datatype": "integer",
       |      "type": "primary_key"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $fragmentPackage
       |
       |case class FragmentClass(
       |id: _root_.scala.Long)
       |
       |object FragmentClass {
       |  implicit val encoder: _root_.io.circe.Encoder[FragmentClass] =
       |    fragmentClass => _root_.io.circe.Json.obj(
       |      "id" -> _root_.io.circe.Encoder[_root_.scala.Long].apply(fragmentClass.id)
       |    )
       |
       |  def init(
       |    id: _root_.scala.Long): FragmentClass = {
       |    FragmentClass(
       |      id = id)
       |  }
       |}
       |""".stripMargin
  )

  // Sad table cases
  it should behave like checkFailedTableGeneration(
    "catch invalid table payloads",
    """{ "id": "foobar", "nodes": [] }""",
    "failed cursor"
  )
  it should behave like checkFailedTableGeneration(
    "catch invalid table identifiers",
    """{ "name": "123tableOne", "columns": [] }""",
    "not a valid Jade identifier"
  )
  it should behave like checkFailedTableGeneration(
    "catch invalid column identifiers",
    """{ "name": "ok_table", "columns": [{ "name": "bad-column", "datatype": "string" }] }""",
    "not a valid Jade identifier"
  )

  // Happy struct cases
  it should behave like checkStructGeneration(
    "generate a zero-field struct",
    s"""{
       |  "name": "no_fields",
       |  "fields": []
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class NoFields()
       |
       |object NoFields {
       |  implicit val encoder: _root_.io.circe.Encoder[NoFields] = { noFields =>
       |    val jsonObj = _root_.io.circe.Json.obj(
       |    )
       |    _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
       |  }
       |}
       |""".stripMargin
  )
  it should behave like checkStructGeneration(
    "generate a one-field struct",
    s"""{
       |  "name": "one_field",
       |  "fields": [
       |    {
       |      "name": "test_field",
       |      "datatype": "string"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class OneField(
       |testField: _root_.scala.Option[_root_.java.lang.String])
       |
       |object OneField {
       |  implicit val encoder: _root_.io.circe.Encoder[OneField] = { oneField =>
       |    val jsonObj = _root_.io.circe.Json.obj(
       |      "test_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(oneField.testField)
       |    )
       |    _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
       |  }
       |}
       |""".stripMargin
  )
  it should behave like checkStructGeneration(
    "generate every type of field",
    s"""{
       |  "name": "all_fields",
       |  "fields": [
       |    {
       |      "name": "bool_field",
       |      "datatype": "boolean"
       |    },
       |    {
       |      "name": "float_field",
       |      "datatype": "float"
       |    },
       |    {
       |      "name": "int_field",
       |      "datatype": "integer"
       |    },
       |    {
       |      "name": "string_field",
       |      "datatype": "string"
       |    },
       |    {
       |      "name": "date_field",
       |      "datatype": "date"
       |    },
       |    {
       |      "name": "timestamp_field",
       |      "datatype": "timestamp"
       |    },
       |    {
       |      "name": "dir_field",
       |      "datatype": "dirref"
       |    },
       |    {
       |      "name": "file_field",
       |      "datatype": "fileref"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class AllFields(
       |boolField: _root_.scala.Option[_root_.scala.Boolean],
       |floatField: _root_.scala.Option[_root_.scala.Double],
       |intField: _root_.scala.Option[_root_.scala.Long],
       |stringField: _root_.scala.Option[_root_.java.lang.String],
       |dateField: _root_.scala.Option[_root_.java.time.LocalDate],
       |timestampField: _root_.scala.Option[_root_.java.time.OffsetDateTime],
       |dirField: _root_.scala.Option[_root_.java.lang.String],
       |fileField: _root_.scala.Option[_root_.java.lang.String])
       |
       |object AllFields {
       |  implicit val encoder: _root_.io.circe.Encoder[AllFields] = { allFields =>
       |    val jsonObj = _root_.io.circe.Json.obj(
       |      "bool_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Boolean]].apply(allFields.boolField),
       |      "float_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Double]].apply(allFields.floatField),
       |      "int_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Long]].apply(allFields.intField),
       |      "string_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allFields.stringField),
       |      "date_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.time.LocalDate]].apply(allFields.dateField),
       |      "timestamp_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.time.OffsetDateTime]].apply(allFields.timestampField),
       |      "dir_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allFields.dirField),
       |      "file_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allFields.fileField)
       |    )
       |    _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
       |  }
       |}
       |""".stripMargin
  )
  it should "output compile-able struct code with all types" in {
    """case class AllFields(
        boolField: _root_.scala.Option[_root_.scala.Boolean],
        floatField: _root_.scala.Option[_root_.scala.Double],
        intField: _root_.scala.Option[_root_.scala.Long],
        stringField: _root_.scala.Option[_root_.java.lang.String],
        dateField: _root_.scala.Option[_root_.java.time.LocalDate],
        timestampField: _root_.scala.Option[_root_.java.time.OffsetDateTime],
        dirField: _root_.scala.Option[_root_.java.lang.String],
        fileField: _root_.scala.Option[_root_.java.lang.String])

        object AllFields {
          implicit val encoder: _root_.io.circe.Encoder[AllFields] = { allFields =>
            val jsonObj = _root_.io.circe.Json.obj(
              "bool_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Boolean]].apply(allFields.boolField),
              "float_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Double]].apply(allFields.floatField),
              "int_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Long]].apply(allFields.intField),
              "string_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allFields.stringField),
              "date_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.time.LocalDate]].apply(allFields.dateField),
              "timestamp_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.time.OffsetDateTime]].apply(allFields.timestampField),
              "dir_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allFields.dirField),
              "file_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.java.lang.String]].apply(allFields.fileField)
            )
            _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
          }
        }
       """ should compile
  }
  it should behave like checkStructGeneration(
    "generate required fields",
    s"""{
       |  "name": "required_field",
       |  "fields": [
       |    {
       |      "name": "test_required",
       |      "datatype": "string",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class RequiredField(
       |testRequired: _root_.java.lang.String)
       |
       |object RequiredField {
       |  implicit val encoder: _root_.io.circe.Encoder[RequiredField] = { requiredField =>
       |    val jsonObj = _root_.io.circe.Json.obj(
       |      "test_required" -> _root_.io.circe.Encoder[_root_.java.lang.String].apply(requiredField.testRequired)
       |    )
       |    _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
       |  }
       |}
       |""".stripMargin
  )
  it should behave like checkStructGeneration(
    "generate array fields",
    s"""{
       |  "name": "array_field",
       |  "fields": [
       |    {
       |      "name": "test_array",
       |      "datatype": "float",
       |      "type": "repeated"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class ArrayField(
       |testArray: _root_.scala.collection.immutable.List[_root_.scala.Double])
       |
       |object ArrayField {
       |  implicit val encoder: _root_.io.circe.Encoder[ArrayField] = { arrayField =>
       |    val jsonObj = _root_.io.circe.Json.obj(
       |      "test_array" -> _root_.io.circe.Encoder[_root_.scala.collection.immutable.List[_root_.scala.Double]].apply(arrayField.testArray)
       |    )
       |    _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
       |  }
       |}
       |""".stripMargin
  )
  it should behave like checkStructGeneration(
    "mix all kinds of column fields",
    s"""{
       |  "name": "all_modifiers",
       |  "fields": [
       |    {
       |      "name": "key_field",
       |      "datatype": "date",
       |      "type": "primary_key"
       |    },
       |    {
       |      "name": "normal_field",
       |      "datatype": "boolean"
       |    },
       |    {
       |      "name": "array_field",
       |      "datatype": "integer",
       |      "type": "repeated"
       |    },
       |    {
       |      "name": "required_field",
       |      "datatype": "float",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class AllModifiers(
       |keyField: _root_.java.time.LocalDate,
       |normalField: _root_.scala.Option[_root_.scala.Boolean],
       |arrayField: _root_.scala.collection.immutable.List[_root_.scala.Long],
       |requiredField: _root_.scala.Double)
       |
       |object AllModifiers {
       |  implicit val encoder: _root_.io.circe.Encoder[AllModifiers] = { allModifiers =>
       |    val jsonObj = _root_.io.circe.Json.obj(
       |      "key_field" -> _root_.io.circe.Encoder[_root_.java.time.LocalDate].apply(allModifiers.keyField),
       |      "normal_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Boolean]].apply(allModifiers.normalField),
       |      "array_field" -> _root_.io.circe.Encoder[_root_.scala.collection.immutable.List[_root_.scala.Long]].apply(allModifiers.arrayField),
       |      "required_field" -> _root_.io.circe.Encoder[_root_.scala.Double].apply(allModifiers.requiredField)
       |    )
       |    _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
       |  }
       |}
       |""".stripMargin
  )
  it should "output compile-able struct code with all modifiers" in {
    """case class AllModifiers(
        keyField: _root_.java.time.LocalDate,
        normalField: _root_.scala.Option[_root_.scala.Boolean],
        arrayField: _root_.scala.collection.immutable.List[_root_.scala.Long],
        requiredField: _root_.scala.Double)

        object AllModifiers {
          implicit val encoder: _root_.io.circe.Encoder[AllModifiers] = { allModifiers =>
            val jsonObj = _root_.io.circe.Json.obj(
              "key_field" -> _root_.io.circe.Encoder[_root_.java.time.LocalDate].apply(allModifiers.keyField),
              "normal_field" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Boolean]].apply(allModifiers.normalField),
              "array_field" -> _root_.io.circe.Encoder[_root_.scala.collection.immutable.List[_root_.scala.Long]].apply(allModifiers.arrayField),
              "required_field" -> _root_.io.circe.Encoder[_root_.scala.Double].apply(allModifiers.requiredField)
            )
            _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
          }
        }
       """ should compile
  }

  it should behave like checkStructGeneration(
    "escape columns named `type` in generated struct code",
    s"""{
       |  "name": "type_field",
       |  "fields": [
       |    {
       |      "name": "type",
       |      "datatype": "float"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class TypeField(
       |`type`: _root_.scala.Option[_root_.scala.Double])
       |
       |object TypeField {
       |  implicit val encoder: _root_.io.circe.Encoder[TypeField] = { typeField =>
       |    val jsonObj = _root_.io.circe.Json.obj(
       |      "type" -> _root_.io.circe.Encoder[_root_.scala.Option[_root_.scala.Double]].apply(typeField.`type`)
       |    )
       |    _root_.io.circe.Json.fromString(jsonObj.dropNullValues.noSpaces)
       |  }
       |}
       |""".stripMargin
  )
  it should "output compile-able struct code with escaped fields" in {
    """case class TypeField(
       `type`: _root_.scala.Option[_root_.scala.Double])
       """ should compile
  }

  // Sad struct cases
  it should behave like checkFailedStructGeneration(
    "catch invalid struct payloads",
    """{ "id": "foobar", "nodes": [] }""",
    "failed cursor"
  )
  it should behave like checkFailedStructGeneration(
    "catch invalid struct identifiers",
    """{ "name": "1231structOne", "fields": [] }""",
    "not a valid Jade identifier"
  )
  it should behave like checkFailedStructGeneration(
    "catch invalid field identifiers",
    """{ "name": "ok_struct", "fields": [{ "name": "bad-field", "datatype": "string" }] }""",
    "not a valid Jade identifier"
  )
}
