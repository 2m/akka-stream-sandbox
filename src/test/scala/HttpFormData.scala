import akka.actor.ActorSystem
import akka.http.Http
import akka.http.model._
import akka.http.server.Directives
import akka.http.marshalling.Marshal
import akka.http.model.Multipart.FormData
import akka.http.model.HttpEntity.Strict
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.net.InetSocketAddress
import org.scalatest._

class HttpFormData extends WordSpec with Matchers with Directives {
  
  /*"http client" should {
    "get response" when {
      "asked for string data" in {
        httpResponseFor(dataUri) shouldBe a[HttpResponse]
      }
      "asked for form data" in {
        httpResponseFor(formDataUri) shouldBe a[HttpResponse]
      }
    }
  }
  
  val endpoint = new InetSocketAddress("127.0.0.1", 8080)
  val host = endpoint.getHostName
  val port = endpoint.getPort
  
  val formDataUri = Uri(s"http://$host:$port/formData")
  val dataUri = Uri(s"http://$host:$port/data")
  
  implicit val system = ActorSystem()
  implicit val mat = ActorFlowMaterializer()
  import system.dispatcher
  
  val body = FormData.BodyPart.Strict(
    "name",
    Strict(MediaTypes.`application/octet-stream`, ByteString("ohi")),
    Map("filename" -> "filename")
  )
  
  Http(system).bind(host, port).startHandlingWith {
    path("formData") {
      complete {
        Marshal(FormData(body)).to[HttpResponse]
      }
    } ~
    path("data") {
      complete {
        Marshal("ohi").to[HttpResponse]
      }
    }
  }
  
  def httpResponseFor(uri: Uri) = {
    val connection = Http(system).outgoingConnection(host, port)
    val responseFuture = Source.single(HttpRequest(uri = uri)).via(connection.flow).runWith(Sink.head)
    Await.result(responseFuture, Duration.Inf)
  }*/
  
}
