package org.broadinstitute.monster.common

import java.time.{LocalDate, OffsetDateTime}

import com.spotify.scio.coders.Coder
import org.broadinstitute.monster.common.msg.UpackMsgCoder
import upack.Msg

/** Mixin containing common Scio Coders used across our pipelines. */
trait PipelineCoders {

  implicit val msgCoder: Coder[Msg] = Coder.beam(new UpackMsgCoder)

  implicit val dateCoder: Coder[LocalDate] = Coder.xmap(Coder.stringCoder)(
    LocalDate.parse(_),
    _.toString
  )

  implicit val odtCoder: Coder[OffsetDateTime] = Coder.xmap(Coder.stringCoder)(
    OffsetDateTime.parse(_),
    _.toString
  )
}
