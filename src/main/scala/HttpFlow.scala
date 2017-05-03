import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RouteResult}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.{ActorAttributes, ActorMaterializer}
import akka.stream.scaladsl._
import akka.util.ByteString
import spray.json._
import DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling._

object HttpFlow extends App with Directives {

  implicit val sys = ActorSystem()
  implicit val mat = ActorMaterializer()
  import sys.dispatcher

  val flow = Flow[(Int, String)].map { case (param, cookie) => s"Param: $param and cookie $cookie" }

  implicit def stringStreamMarshaller(implicit ec: ExecutionContext): ToResponseMarshaller[Source[String, Any]] =
    Marshaller.withOpenCharset(MediaTypes.`text/plain`) { (s, cs) =>
      println("Marshaller thread: " + Thread.currentThread.getName)
      HttpResponse(entity = HttpEntity.CloseDelimited(ContentTypes.`text/plain(UTF-8)`, s.map(ByteString(_))))
    }

  case class Param(s: String)

  implicit val paramUnmarshall = Unmarshaller.strict[String, Param](Param)

  val routes = {
    path("test") {
      get {
        parameter('param.as[Param]) { param ⇒
          complete(param.s)
        } ~
        complete {
          println("Route thread: " + Thread.currentThread.getName)
          Source.single(123, "crunchy").via(flow)
        }
      } ~
      post {
        val map: Map[String, Any] = Map("hi" -> Map("nice" -> 1))
        implicit val mapMarshaller2: ToEntityMarshaller[Map[String, Any]] = Marshaller.opaque { map =>
          HttpEntity(ContentType(MediaTypes.`application/json`), map.toString)
        }
        complete(StatusCodes.OK -> map)
      }
    } ~
    path("hi") {
      complete("Ohi")
    }
  }

  val routeFlow = RouteResult
    .route2HandlerFlow(routes)
    .addAttributes(ActorAttributes.dispatcher("my-dispatcher"))
  Http().bindAndHandle(routes, "127.0.0.1", 9000)

  /*Http().bind("127.0.0.1", 9000).runForeach { conn =>
    conn.flow.join(routeFlow).run()
  }*/

}
