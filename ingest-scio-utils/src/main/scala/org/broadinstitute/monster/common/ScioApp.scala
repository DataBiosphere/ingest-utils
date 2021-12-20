package org.broadinstitute.monster.common

import caseapp.core.help.Help
import caseapp.core.parser.Parser
import com.spotify.scio.{ContextAndArgs, ScioResult}

import scala.annotation.nowarn

/**
  * @param parser implicit parser doesn't need to be passed, derived from Args type
  * @param help help generated from the Args type
  * @tparam Args an Args case class with input definitions and help messages
  */
// suppress caseapp deprecation for now
@nowarn("cat=deprecation")
abstract class ScioApp[Args](
  implicit parser: Parser[Args],
  help: Help[Args]
) {
  /** Builder which can convert command-line args into a pipeline. */
  def pipelineBuilder: PipelineBuilder[Args]

  /** a post processing function if so desired that operates on a ScioResult */
  def postProcess: ScioResult => Unit = _ => ()

  /**
    * Entry-point for scio CLPs.
    *
    * Parses command-line args, constructs a pipeline, runs the
    * pipeline, and waits for the pipeline to finish.
    */
  def main(args: Array[String]): Unit = {
    val (pipelineContext, parsedArgs) =
      ContextAndArgs.typed[Args](args)
    pipelineBuilder.buildPipeline(pipelineContext, parsedArgs)
    val result = pipelineContext.run().waitUntilDone()
    postProcess(result)
  }
}
