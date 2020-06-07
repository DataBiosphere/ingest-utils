package org.broadinstitute.monster.common

import caseapp.Name
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter
import com.spotify.scio.ContextAndArgs

/** Entry-point for command-line applications that run scio pipelines. */
abstract class ScioApp[Args](implicit parser: Parser[Args], help: Help[Args]) {
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
  final def main(args: Array[String]): Unit = {
    val (pipelineContext, parsedArgs) =
      ContextAndArgs.typed[Args](args)(parser.nameFormatter(argFormatter), help)
    pipelineBuilder.buildPipeline(pipelineContext, parsedArgs)
    pipelineContext.run().waitUntilDone()
    ()
  }
}
