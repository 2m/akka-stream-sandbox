import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._
import scala.concurrent.Future

class FlowWrap extends WordSpec with Matchers with ScalaFutures {

  implicit val sys = ActorSystem("FlowWrap")
  implicit val mat = ActorMaterializer()

  "flow wrap" should {
    "wrap" in {
      val wrapped: Flow[Int, String, (Future[Int], Cancellable)] = Flow.wrap(Sink.head[Int], Source(0.seconds, 100.millis, "tick"))(Keep.both)

      val ((pub, (future, scheduler)), sub) = TestSource.probe[Int].viaMat(wrapped)(Keep.both).toMat(TestSink.probe[String])(Keep.both).run()

      sub.request(3)
      sub.expectNext("tick")
      sub.expectNext("tick")
      sub.expectNext("tick")

      pub.sendNext(2)
      sub.expectNoMsg()

      scheduler.cancel()
      sub.expectComplete()

      whenReady(future)(_ shouldBe 2)
    }
  }

}
