package org.broadinstitute.monster.common

import caseapp.Name
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter
import com.spotify.scio.{ContextAndArgs, ScioResult}

/**
  * @param parser implicit parser doesn't need to be passed, derived from Args type
  * @param help help generated from the Args type
  * @param postProcess a post processing function if so desired
  * @tparam Args an Args case class with input definitions and help messages
  */
abstract class ScioApp[Args](
  implicit parser: Parser[Args],
  help: Help[Args],
  postProcess: ScioResult => Unit = _ => ()
) {
  /** Builder which can convert command-line args into a pipeline. */
  def pipelineBuilder: PipelineBuilder[Args]

  /** Formatter for CLP arguments which retains the formatting of the Scala name. */
  private val argFormatter: Formatter[Name] = name => name.name

  /**
    * Entry-point for scio CLPs.
    *
    * Parses command-line args, constructs a pipeline, runs the
    * pipeline, and waits for the pipeline to finish.
    */
  def main(args: Array[String]): Unit = {
    val (pipelineContext, parsedArgs) =
      ContextAndArgs.typed[Args](args)(parser.nameFormatter(argFormatter), help)
    pipelineBuilder.buildPipeline(pipelineContext, parsedArgs)
    val result = pipelineContext.run().waitUntilDone()
    postProcess(result)
  }
}
