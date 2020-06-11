package org.broadinstitute.monster.common

import com.spotify.scio.ScioContext

/**
  * Interface for types that can use some set of command-line
  * arguments to construct a scio pipeline.
  *
  * @tparam Args container for command-line args powering
  *              pipeline construction
  */
trait PipelineBuilder[Args] extends PipelineCoders {
  /**
    * Use a set of command-line args to construct a scio
    * pipeline within a pipeline context.
    *
    * Does not execute the constructed pipeline.
    */
  def buildPipeline(ctx: ScioContext, args: Args): Unit
}
