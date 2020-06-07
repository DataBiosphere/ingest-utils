package org.broadinstitute.monster.common.msg

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import upack._

class JsonParserSpec extends AnyFlatSpec with Matchers {
  behavior of "JsonParser"

  it should "parse JSON bools" in {
    JsonParser.parseEncodedJson("true") shouldBe Bool(true)
    JsonParser.parseEncodedJson("false") shouldBe Bool(false)
  }

  it should "parse JSON ints" in {
    JsonParser.parseEncodedJson("0") shouldBe Int64(0L)
    JsonParser.parseEncodedJson("-99999") shouldBe Int64(-99999)
    JsonParser.parseEncodedJson("1234567") shouldBe Int64(1234567)
  }

  it should "parse JSON floats" in {
    JsonParser.parseEncodedJson("0.9876") shouldBe Float64(0.9876d)
    JsonParser.parseEncodedJson("123.965") shouldBe Float64(123.965d)
    JsonParser.parseEncodedJson("-555.444") shouldBe Float64(-555.444)
  }

  it should "parse JSON nulls" in {
    JsonParser.parseEncodedJson("null") shouldBe Null
  }

  it should "parse JSON strings" in {
    JsonParser.parseEncodedJson("\"\"") shouldBe Str("")
    JsonParser.parseEncodedJson("\"foo\"") shouldBe Str("foo")
  }

  it should "parse JSON objects" in {
    JsonParser.parseEncodedJson("{}") shouldBe Obj()
    JsonParser.parseEncodedJson("""{"foo": "bar", "n": 123}""") shouldBe Obj(
      Str("foo") -> Str("bar"),
      Str("n") -> Int64(123L)
    )
  }

  it should "parse JSON arrays" in {
    JsonParser.parseEncodedJson("[]") shouldBe Arr()
    JsonParser.parseEncodedJson("[1, 1.23, -0.8]") shouldBe Arr(
      Int64(1L),
      Float64(1.23d),
      Float64(-0.8d)
    )
    JsonParser.parseEncodedJson("""[{"foo": "bar", "ok": true}]""") shouldBe Arr(
      Obj(Str("foo") -> Str("bar"), Str("ok") -> Bool(true))
    )
  }
}
