package org.broadinstitute.monster.sbt.jade.model.jadeapi

import io.circe.Encoder
import io.circe.derivation.deriveEncoder
import org.broadinstitute.monster.sbt.jade.model.JadeIdentifier

case class JadeRelationshipRef(
  table: JadeIdentifier,
  column: JadeIdentifier
)

object JadeRelationshipRef {
  implicit val encoder: Encoder[JadeRelationshipRef] = deriveEncoder
}
