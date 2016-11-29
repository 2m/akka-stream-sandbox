import akka.actor.ActorSystem
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.{AsyncWordSpec, Matchers}

import scala.collection.immutable
import scala.concurrent.Future
import scala.util.control.NonFatal

class StreamResultOrFailure extends AsyncWordSpec with Matchers {

  implicit val sys = ActorSystem("StreamResultOrFailure")
  implicit val mat = ActorMaterializer()

  "stream" should {

    val err = new Error("got 4")

    def run(elements: immutable.Seq[Int]): Future[Either[Throwable, Int]] = {
      val sinkStrategy: Supervision.Decider = {
        case NonFatal(ex) => Supervision.Restart
        case err => Supervision.Stop
      }

      Source(elements)
        .map { el =>
          if (el == 4) throw err
          else el
        }
        .runWith(Sink.fold[Int, Int](0)(_ + _).withAttributes(ActorAttributes.supervisionStrategy(sinkStrategy)))
        .map(Right(_))
        .recover {
          case t => Left(t.getCause)
        }
    }

    "end with a value" in {
      run(immutable.Seq(1, 2, 3)).map(_.right.get shouldBe 6)
    }

    "end with a failure" in {
      run(immutable.Seq(1, 2, 3, 4)).map(_.left.get shouldBe err)
    }
  }

}
