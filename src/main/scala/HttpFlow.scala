import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{ Marshaller, ToResponseMarshaller }
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString


object HttpFlow extends App with Directives {

  implicit val sys = ActorSystem()
  implicit val mat = ActorFlowMaterializer()
  import sys.dispatcher

  val flow = Flow[(Int, String)].map { case (param, cookie) => s"Param: $param and cookie $cookie" }

  implicit def stringStreamMarshaller(implicit ec: ExecutionContext): ToResponseMarshaller[Source[String, Any]] =
    Marshaller.withFixedCharset(MediaTypes.`text/plain`, HttpCharsets.`UTF-8`) { s =>
      HttpResponse(entity = HttpEntity.CloseDelimited(MediaTypes.`text/plain`, s.map(ByteString(_))))
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
          Source.single(123, "crunchy").via(flow)
        }
      } ~
      post {
        entity(as[Multipart.FormData]) { entity =>
          val files = entity.parts.mapAsync(parallelism = 4) { bodyPart =>
            ///bodyPart.entity.dataBytes.runWith(Sink.file(...)) when https://github.com/akka/akka/pull/17211 is merged...
            bodyPart.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { contents =>
              s"Received file: ${bodyPart.filename} with contents:\n$contents"
            }
          }
          complete(files)
        }
      }
    }
  }

  Http().bindAndHandle(routes, "127.0.0.1", 9000)

}
