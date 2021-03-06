package org.broadinstitute.monster.sbt.jade.model

import io.circe.{Decoder, Encoder}

/**
  * Identifier for a table/column in a Jade dataset.
  *
  * We restrict how this wrapper can be constructed to enforce
  * that the ID string only contains allowed characters.
  */
class JadeIdentifier private[sbt] (private[sbt] val id: String) {
  override def toString: String = id.toString

  override def equals(obj: Any): Boolean =
    obj match {
      case other: JadeIdentifier => id.equals(other.id)
      case _                     => false
    }
  override def hashCode(): Int = id.hashCode
}

object JadeIdentifier {
  /** Pattern matching valid Jade IDs. */
  private val idPattern = "^[a-z][a-z0-9_]{0,62}$".r

  implicit val encoder: Encoder[JadeIdentifier] =
    Encoder[String].contramap(_.id)

  implicit val decoder: Decoder[JadeIdentifier] =
    Decoder[String].emap(fromString)

  /**
    * Attempt to parse a raw string into a Jade ID, returning
    * an error message if the string is not valid.
    */
  def fromString(id: String): Either[String, JadeIdentifier] = {
    val matcher = idPattern.pattern.matcher(id)
    Either.cond(
      matcher.matches(),
      new JadeIdentifier(id),
      s"String '$id' is not a valid Jade identifier"
    )
  }
}
