import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.net.InetSocketAddress
import org.scalatest._

class HttpFormData extends WordSpec with Matchers with Directives {

  "http client" should {
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
  implicit val mat = ActorMaterializer()
  import system.dispatcher

  val body = FormData.BodyPart.Strict(
    "name",
    Strict(MediaTypes.`application/octet-stream`, ByteString("ohi")),
    Map("filename" -> "filename")
  )

  Http(system).bindAndHandle(
    path("formData") {
      complete {
        Marshal(FormData(body)).to[HttpResponse]
      }
    } ~
    path("data") {
      complete {
        Marshal("ohi").to[HttpResponse]
      }
    }, host, port)

  def httpResponseFor(uri: Uri) = {
    val connection = Http(system).outgoingConnection(host, port)
    val responseFuture = Source.single(HttpRequest(uri = uri)).via(connection).runWith(Sink.head)
    Await.result(responseFuture, Duration.Inf)
  }

}
