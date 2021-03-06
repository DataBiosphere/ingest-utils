package org.broadinstitute.monster.common

import upack._

package object msg {

  /**
    * Extension methods for pulling fields out of upack Msgs.
    *
    * These should all be bridges to methods defined in `MsgTransformations`.
    */
  implicit class MsgOps(val msg: Msg) extends AnyVal {

    def extract[V: MsgParser](fieldChain: String*): V =
      MsgOperations.extract(msg, fieldChain)

    def tryExtract[V: MsgParser](fieldChain: String*): Option[V] =
      MsgOperations.tryExtract(msg, fieldChain)

    def read[V: MsgParser](fieldChain: String*): V =
      MsgOperations.read(msg, fieldChain)

    def tryRead[V: MsgParser](fieldChain: String*): Option[V] =
      MsgOperations.tryRead(msg, fieldChain)
  }
}
