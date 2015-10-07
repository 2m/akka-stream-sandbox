import akka.actor._
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl.TestSource
import akka.stream.ActorMaterializer
import akka.stream.stage._
import org.scalatest._
import scala.concurrent.Await
import scala.concurrent.duration._

object StageFailureSpec {

  class BoomStage extends PushPullStage[String, Int] {

    var buf = Vector.empty[String]

    final override def onPush(elem: String, ctx: Context[Int]): SyncDirective = elem match {
      case "complete" =>
        ctx.pushAndFinish(buf.size)
      case "boom" =>
        ctx.fail(new Error("boom"))
      case msg =>
        buf :+= (msg + " processed")
        ctx.pull()
    }

    final override def onPull(ctx: Context[Int]): SyncDirective = {
      ctx.pull()
    }
  }

}

class StageFailureSpec extends fixture.WordSpec with Matchers {
  import StageFailureSpec._

  case class FixtureParam()(implicit val sys: ActorSystem, implicit val mat: ActorMaterializer)

  def withFixture(test: OneArgTest) = {

    implicit val sys = ActorSystem("HttpClientFollowSpec")
    implicit val mat = ActorMaterializer()

    try {
      withFixture(test.toNoArgTest(FixtureParam()(sys, mat))) // "loan" the fixture to the test
    }
    finally {
      sys.shutdown()
      sys.awaitTermination()
    }
  }

  "custom PushPullStage" should {
    "complete stream successfully" in { f =>
      import f._
      val (probe, future) = TestSource.probe[String].transform(() => new BoomStage).toMat(Sink.head)(Keep.both).run()

      probe.sendNext("one")
      probe.sendNext("two")
      probe.sendNext("three")

      probe.sendNext("complete")

      Await.result(future, 1.second) shouldBe 3
    }

    "fail stream with error" in { f =>
      import f._
      val (probe, future) = TestSource.probe[String].transform(() => new BoomStage).toMat(Sink.head)(Keep.both).run()

      probe.sendNext("one")
      probe.sendNext("two")
      probe.sendNext("three")

      probe.sendNext("boom")

      Await.result(future.failed, 1.second).getCause.getMessage shouldBe "boom"
    }
  }
}
