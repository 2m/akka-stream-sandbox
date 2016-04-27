import akka.actor._
import akka.stream._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.StdIn

object Main extends App {
  import Client._

  implicit val sys = ActorSystem("Main")
  implicit val mat = ActorMaterializer()
  import sys.dispatcher

  request("_bulk", "localhost", 9200).onComplete(println)

  StdIn.readLine()
  Await.ready(sys.terminate(), 10.seconds)
}
