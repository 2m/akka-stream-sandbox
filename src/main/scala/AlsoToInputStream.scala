import java.io._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import scala.concurrent.Await
import scala.concurrent.duration._

object AlsoToInputStream extends App {

  implicit val sys = ActorSystem("AlsoToInputStream")
  implicit val mat = ActorMaterializer()

  import sys.dispatcher

  val filename = "/home/martynas/Downloads/G0037140.JPG"
  val input = new FileInputStream(filename)
  val src = StreamConverters.fromInputStream(() => input)

  // input streams
  val pipedIn = new PipedInputStream()
  val pipedOut = new PipedOutputStream(pipedIn)
  val pipedIn2 = new PipedInputStream()
  val pipedOut2 = new PipedOutputStream(pipedIn2)

  // with two stream converters, piped data blocks on reading
  src
    .alsoTo(StreamConverters.fromOutputStream(() => pipedOut))
    .runWith(StreamConverters.fromOutputStream(() => pipedOut2))

  val bytesRead = StreamConverters.fromInputStream(() => pipedIn).toMat(Sink.ignore)(Keep.left).run()
  val bytesRead2 = StreamConverters.fromInputStream(() => pipedIn2).toMat(Sink.ignore)(Keep.left).run()

  println(Await.result(bytesRead, 10.seconds))
  println(Await.result(bytesRead2, 10.seconds))

  sys.shutdown()
  sys.awaitTermination()

}
