package org.broadinstitute.monster.common

import better.files.File
import com.spotify.scio.testing.PipelineSpec
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.broadinstitute.monster.common.msg.JsonParser
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import upack.Msg

/**
  * Base class for test suites that assert on the output of
  * a single run of a scio pipeline.
  */
abstract class PipelineBuilderSpec[Args] extends PipelineSpec with Matchers with BeforeAndAfterAll {
  def testArgs: Args
  def builder: PipelineBuilder[Args]

  override def beforeAll(): Unit = {
    runWithRealContext(PipelineOptionsFactory.create())(
      builder.buildPipeline(_, testArgs)
    ).waitUntilDone()
    ()
  }

  /**
    * Read all newline-delimited JSON objects stored in
    * a directory (potentially in multiple files) into a
    * set, parsing them along the way.
    */
  def readMsgs(directory: File, pattern: String = "*.json"): Set[Msg] =
    directory
      .glob(pattern)
      .flatMap(_.lineIterator)
      .map(JsonParser.parseEncodedJson)
      .toSet
}
