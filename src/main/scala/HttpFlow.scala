import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.marshalling.{ Marshaller, ToResponseMarshaller }
import akka.http.model._
import akka.http.server.Directives
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

object HttpFlow extends App with Directives {
  
  implicit val sys = ActorSystem()
  implicit val mat = ActorFlowMaterializer()
  import sys.dispatcher
  
  val flow = Flow.empty[(Int, String)].map { case (param, cookie) => s"Param: $param and cookie $cookie" }
  
  implicit def toResponseMarshaller(implicit ec: ExecutionContext): ToResponseMarshaller[Source[String]] =
    Marshaller.withFixedCharset(MediaTypes.`text/plain`, HttpCharsets.`UTF-8`) { messages =>
      HttpResponse(entity = HttpEntity.CloseDelimited(MediaTypes.`text/plain`, messages.map(ByteString(_))))
    }
  
  val routes = {
    path("test") {
      get {
        complete {
          Source.single(123, "crunchy").via(flow)
        }
      }
    }
  }

  Http().bind("127.0.0.1", 9000).startHandlingWith(routes)
  
}
