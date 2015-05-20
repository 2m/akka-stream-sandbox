import akka.actor._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.RouteTest
import akka.http.scaladsl.testkit.TestFrameworkInterface.Scalatest
import org.scalatest.{ Matchers, WordSpec }
import scala.xml.NodeSeq

object ServerResponseFromActor {
  class HtmlActor extends Actor {
    def receive = {
      case Request(complete) => complete(<html><body><p>opapa</p></body></html>)
    }
  }

  case class Request(complete: NodeSeq => Unit)
}

class ServerResponseFromActor extends WordSpec with RouteTest with Scalatest with Matchers {
  import ServerResponseFromActor._

  "server should give a completer function to actor to respond" in {

    val handler = system.actorOf(Props[HtmlActor])
    val route = path("index") {
      get {
        completeWith(instanceOf[NodeSeq]) { completer =>
          handler ! Request(completer)
        }
      }
    }

    Get("/index") ~> route ~> check {
      response.status shouldBe OK
      unmarshalValue[String](response.entity) shouldBe "<html><body><p>opapa</p></body></html>"
    }
  }
}
