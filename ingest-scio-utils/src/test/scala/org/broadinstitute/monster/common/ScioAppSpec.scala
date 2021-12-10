package org.broadinstitute.monster.common

import java.time.{LocalDate, LocalTime, OffsetDateTime, ZoneOffset}
import better.files.File
import org.apache.beam.sdk.options.PipelineOptions
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import upack._

class ScioAppSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach with PipelineCoders {
  val tmpOut = File.newTemporaryDirectory()

  val app = new ScioApp[ScioAppSpec.Args] {

    override val pipelineBuilder = (ctx, args) => {
      ctx
        .parallelize(List(args.getValue))
        .map { i =>
          if (i < 0) throw new RuntimeException("AHH!")
          val date = LocalDate.of(2020, i, 1)
          (Int32(i): Msg, date, OffsetDateTime.of(date, LocalTime.of(0, 0), ZoneOffset.UTC))
        }
        .map(_._1.int32)
        .saveAsTextFile(tmpOut.pathAsString, numShards = 1, suffix = ".value")
      ()
    }
  }

  override def afterEach(): Unit =
    tmpOut.list.foreach(_.delete())

  behavior of "ScioApp"

  it should "run pipelines to completion" in {
    app.main(Array("--value=10"))
    val foo = tmpOut.list.toArray
    println(foo)
    tmpOut.list.toArray.head.contentAsString shouldBe "10\n"
  }

  it should "raise an error if pipelines fail" in {
    an[Exception] shouldBe thrownBy {
      app.main(Array("--value=-1"))
    }
  }

  it should "properly parse args as camelCase" in {
    app.main(Array("--value=10", "--camelCaseValue=foo"))
    tmpOut.list.toArray.head.contentAsString shouldBe "10\n"
  }
}

object ScioAppSpec {

  trait Args extends PipelineOptions {
    def getValue: Int
    def setValue(input: Int): Unit

    def getCamelCaseValue: String
    def setCamelCaseValue(v: String): Unit
  }
}
