package org.broadinstitute.monster.common

import com.spotify.scio.ContextAndArgs.TypedArgsParser
import com.spotify.scio.{ContextAndArgs, ScioResult}
import org.apache.beam.sdk.options.PipelineOptions
import com.spotify.scio._

import scala.util.Try

/**
  * @param parser implicit parser doesn't need to be passed, derived from Args type
  * @param help help generated from the Args type
  * @tparam Args an Args case class with input definitions and help messages
  */
abstract class ScioApp[Arguments <: PipelineOptions] {
  /** Builder which can convert command-line args into a pipeline. */
  def pipelineBuilder: PipelineBuilder[Arguments]

  /** a post processing function if so desired that operates on a ScioResult */
  def postProcess: ScioResult => Unit = _ => ()

  /**
    * Entry-point for scio CLPs.
    *
    * Parses command-line args, constructs a pipeline, runs the
    * pipeline, and waits for the pipeline to finish.
    */
  def main(args: Array[String])(implicit tap: TypedArgsParser[Arguments, Try]): Unit = {
    val (pipelineContext, parsedArgs) =
      ContextAndArgs.typed[Arguments](args)

    pipelineBuilder.buildPipeline(pipelineContext, parsedArgs)
    val result = pipelineContext.run().waitUntilDone()
    postProcess(result)
  }
}
