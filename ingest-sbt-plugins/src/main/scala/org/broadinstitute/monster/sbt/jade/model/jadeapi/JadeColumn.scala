package org.broadinstitute.monster.sbt.jade.model.jadeapi

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}
import org.broadinstitute.monster.sbt.jade.model.{DataType, JadeIdentifier}

case class JadeColumn(
  name: JadeIdentifier,
  datatype: DataType,
  arrayOf: Boolean
)

object JadeColumn {
  implicit val encoder: Encoder[JadeColumn] = deriveEncoder(renaming.snakeCase, None)
}
