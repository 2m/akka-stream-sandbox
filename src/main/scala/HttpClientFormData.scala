import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.Future

object HttpClientFormData extends App {

  implicit val sys = ActorSystem("HttpClientFormData")
  implicit val mat = ActorMaterializer()
  implicit val exc = sys.dispatcher

  val uri = Uri("http://putsreq.com/HYzYYhcOxrhIxsezap3U")
  val data = Map("session.id" -> "123", "name" -> "john")

  val request: Future[HttpRequest] = Marshal((HttpMethods.POST, uri, FormData(data))).to[HttpRequest]
  val response = request.flatMap(Http().singleRequest(_))
  response.onComplete(println)
}
