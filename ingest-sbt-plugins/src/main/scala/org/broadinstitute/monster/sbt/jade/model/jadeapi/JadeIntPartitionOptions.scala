package org.broadinstitute.monster.sbt.jade.model.jadeapi

import io.circe.Encoder
import io.circe.derivation.deriveEncoder
import org.broadinstitute.monster.sbt.jade.model.JadeIdentifier

case class JadeIntPartitionOptions(
  column: JadeIdentifier,
  min: Long,
  max: Long,
  interval: Long
)

object JadeIntPartitionOptions {
  implicit val encoder: Encoder[JadeIntPartitionOptions] = deriveEncoder
}
