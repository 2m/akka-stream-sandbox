import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentType, ContentTypes, StatusCodes}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.RouteTest
import akka.http.scaladsl.testkit.TestFrameworkInterface.Scalatest
import org.scalatest.{Matchers, WordSpec}
import spray.json._

object JsonProtocol extends DefaultJsonProtocol {
  import ServerRequestAsJson._
  implicit val requestFormat = jsonFormat3(Request)
}

object ServerRequestAsJson {
  case class Request(a: Int, b: Int, prompt: String)
}

class ServerRequestAsJson extends WordSpec with RouteTest with Scalatest with Matchers {
  import JsonProtocol._
  import SprayJsonSupport._

  "server should parse json request" in {

    val route =
      path("add") {
        post {
          entity(as[ServerRequestAsJson.Request]) { entity =>
            complete(s"${entity.prompt}${entity.a + entity.b}")
          }
        }
      } ~
      path("add_json") {
        post {
          entity(as[ServerRequestAsJson.Request]) { entity =>
            complete(StatusCodes.BadRequest, JsObject("answer" -> JsNumber(entity.a + entity.b)))
          }
        }
      }

    val request = """|{
                     |  "a": 1,
                     |  "b": 2,
                     |  "prompt": "Sum is: "
                     |}""".stripMargin
    Post("/add", request.parseJson) ~> route ~> check {
      response.status shouldBe OK
      unmarshalValue[String](response.entity) shouldBe "Sum is: 3"
    }

    Map().exists

    Post("/add_json", request.parseJson) ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
      contentType shouldBe ContentTypes.`application/json`
      unmarshalValue[JsValue](response.entity) shouldBe JsObject("answer" -> JsNumber(3))
    }
  }
}
