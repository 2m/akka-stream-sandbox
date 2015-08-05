import akka.actor._
import akka.stream.actor._
import akka.stream.scaladsl._
import org.scalatest._

object ActorProcessor {

  class Processor extends Actor with ActorPublisher[String] with ActorSubscriber {

    val MaxBufSize = 10
    var buf = Vector.empty[String]

    override val requestStrategy = new MaxInFlightRequestStrategy(max = MaxBufSize) {
      override def inFlightInternally: Int = buf.size
    }

    def receive = {
      case ActorSubscriberMessage.OnNext(msg: String) => buf :+= (msg + "processed")
      case ActorPublisherMessage.Request(_) => deliverBuf()
      case ActorPublisherMessage.Cancel => context.stop(self)
    }

    @annotation.tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      /*
       * totalDemand is a Long and could be larger than
       * what buf.splitAt can accept
       */
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }
  }

  object Processor {
    def props = Props[Processor]
  }

}

class ActorProcessor extends WordSpec with Matchers {
  import ActorProcessor._

  "Actor implementing ActorSubscriber and ActorPublisher" should {
    "work like a flow" in {
      Flow() { implicit b =>
        val sink = Sink.actorSubscriber(Processor.props)

        ???
      }
    }
  }
}
