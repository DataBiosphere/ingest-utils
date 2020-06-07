package org.broadinstitute.monster.common

import better.files.File

class PipelineBuilderSpecSpec extends PipelineBuilderSpec[PipelineBuilderSpecSpec.Args] {
  val tmpOut = File.newTemporaryDirectory()

  override val testArgs = PipelineBuilderSpecSpec.Args(5)

  override val builder = (ctx, args) => {
    ctx
      .parallelize(List(args.value))
      .saveAsTextFile(tmpOut.pathAsString, numShards = 1, suffix = ".value")
    ()
  }

  override def afterAll(): Unit = tmpOut.delete()

  behavior of "PipelineBuilderSpec"

  it should "build and run the pipeline before any tests, updating the reference" in {
    readMsgs(tmpOut, pattern = "*.value").map(_.int32) shouldBe Set(testArgs.value)
  }
}

object PipelineBuilderSpecSpec {
  case class Args(value: Int)
}
