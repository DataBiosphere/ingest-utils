package org.broadinstitute.monster.common.msg

import io.circe.Json
import upack.Msg
import upickle.core.Visitor

object JsonParser {

  /**
    * Parse encoded JSON into a upack Msg.
    *
    * This method uses a custom transformation method to
    * better preserve distinctions between numeric types.
    */
  def parseEncodedJson(json: String): Msg = {
    val maybeParsed = io.circe.parser.parse(json)
    maybeParsed.fold(
      err => throw new Exception(s"Failed to parse input line as JSON: $json", err),
      js => transform(js, Msg)
    )
  }

  /**
    * Custom transformation method.
    *
    * Largely a copy-paste of code from uPickle, since it doesn't
    * expose hooks for modifying behaviors for individual cases.
    *
    */
  private def transform(json: Json, visitor: Visitor[Msg, Msg]): Msg = json.fold(
    visitor.visitNull(-1),
    if (_) visitor.visitTrue(-1) else visitor.visitFalse(-1),
    n =>
      n.toLong
        .map(visitor.visitInt64(_, -1))
        .getOrElse(visitor.visitFloat64(n.toDouble, -1)),
    visitor.visitString(_, -1),
    arr => {
      val ctx = visitor.visitArray(arr.size, -1).narrow
      // Maximum yuck for `asInstanceOf`, but I can't figure out how to make
      // type inference happy without it. No clue how it works in uPickle.
      val subVisitor = ctx.subVisitor.asInstanceOf[Visitor[Msg, Msg]]
      arr.foreach(item => ctx.visitValue(transform(item, subVisitor), -1))
      ctx.visitEnd(-1)
    },
    obj => {
      val items = obj.toList
      val ctx = visitor.visitObject(items.size, -1).narrow
      // Maximum yuck for `asInstanceOf`, but I can't figure out how to make
      // type inference happy without it. No clue how it works in uPickle.
      val subVisitor = ctx.subVisitor.asInstanceOf[Visitor[Msg, Msg]]
      items.foreach {
        case (k, v) =>
          val keyVisitor = ctx.visitKey(-1)
          ctx.visitKeyValue(keyVisitor.visitString(k, -1))
          ctx.visitValue(transform(v, subVisitor), -1)
      }
      ctx.visitEnd(-1)
    }
  )
}
