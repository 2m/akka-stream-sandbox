import org.scalatest._
import akka.stream.scaladsl._
import akka.stream.scaladsl.Tcp._
import akka.actor._
import akka.util.ByteString
import akka.stream.ActorMaterializer

class StreamTcp extends WordSpec with Matchers {

  implicit val sys = ActorSystem("StreamTcp")
  implicit val mat = ActorMaterializer()
  import sys.dispatcher

  "tcp server stream" should {
    "be able to access binding information" in {

      Tcp().bind("localhost", 0).toMat(Sink.foreach { connection =>
        connection.handleWith(Flow[ByteString])
      })(Keep.left).run().onComplete(println)

      scala.io.StdIn.readLine()

    }
  }
}
