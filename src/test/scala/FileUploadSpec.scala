import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.stream.scaladsl.Source
import akka.stream.ActorMaterializer
import akka.util.ByteString
import scala.concurrent.ExecutionContext
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time._

class FileUploadSpec extends WordSpec with Matchers with Directives with ScalaFutures {

  implicit final def toResponseMarshaller[A](implicit ec: ExecutionContext): ToResponseMarshaller[Source[String, A]] =
    Marshaller.withFixedCharset(MediaTypes.`text/plain`, HttpCharsets.`UTF-8`) { messages =>
      val data = messages.map(ByteString(_))
      HttpResponse(entity = HttpEntity.CloseDelimited(MediaTypes.`text/plain`, data))
    }

  implicit val sys = ActorSystem()
  implicit val mat = ActorMaterializer()
  import sys.dispatcher

  val routes = {
    path("test") {
      post {
        entity(as[Multipart.FormData]) { entity =>
          val messages = entity.parts.mapAsync(parallelism = 4) {
            case bodyPart =>
              bodyPart.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { contents =>
                s"Received file [${bodyPart.name}] with contents [${contents.utf8String}]\n"
              }
          }
          complete(messages)
        }
      }
    }
  }

  "server" should {
    "handle file upload" in {

      val multipartForm = Multipart.FormData(
        Source(List(
          Multipart.FormData.BodyPart.Strict("file1", HttpEntity(ByteString("ohi"))),
          Multipart.FormData.BodyPart("file2", HttpEntity(ContentTypes.`application/octet-stream`, 11, Source.single(ByteString("hello again"))))
        ))
      )

      val content = for {
        binding <- Http().bindAndHandle(routes, "127.0.0.1", 0)
        request <- Marshal(multipartForm).to[RequestEntity]
        response <- Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/test", entity = request))
        entity <- Unmarshal(response.entity).to[String]
      } yield entity

      whenReady(content, Timeout(Span(1, Second))) { s =>
        s shouldBe """|Received file [file1] with contents [ohi]
                      |Received file [file2] with contents [hello again]
                      |""".stripMargin
      }

    }
  }

}
