package org.broadinstitute.monster.common

import better.files.File
import org.apache.beam.sdk.options.{PipelineOptions, PipelineOptionsFactory}

class PipelineBuilderSpecSpec extends PipelineBuilderSpec[PipelineBuilderSpecSpec.Args] {
  val tmpOut = File.newTemporaryDirectory()

  override val testArgs =
    PipelineOptionsFactory.fromArgs("--value=5").as(classOf[PipelineBuilderSpecSpec.Args])

  override val builder = (ctx, args) => {
    ctx
      .parallelize(List(args.getValue))
      .saveAsTextFile(tmpOut.pathAsString, numShards = 1, suffix = ".value")
    ()
  }

  override def afterAll(): Unit = tmpOut.delete()

  behavior of "PipelineBuilderSpec"

  it should "build and run the pipeline before any tests, updating the reference" in {
    readMsgs(tmpOut, pattern = "*.value").map(_.int32) shouldBe Set(testArgs.getValue)
  }
}

object PipelineBuilderSpecSpec {

  trait Args extends PipelineOptions {
    def setValue(i: Int): Unit
    def getValue: Int
  }
}
