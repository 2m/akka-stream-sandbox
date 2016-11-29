import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Await
import scala.concurrent.duration._

object HttpClientResponseNoLimit extends App {

  implicit val sys = ActorSystem("HttpClientResponseNoLimit")
  implicit val mat = ActorMaterializer()
  import sys.dispatcher

  val path = ""
  val host = ""
  val port = 0

  val resp = Source
      .single(HttpRequest(uri = path).withEntity(HttpEntity("")))
      .via(Http().outgoingConnection(host, port))
      .runWith(Sink.head)
      .flatMap(_.entity.dataBytes.runFold(0)(_ + _.size))

  println(Await.result(resp, 2.seconds))
}
