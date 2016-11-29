import org.scalatest._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import Directives._
import scala.concurrent._
import akka.testkit.TestProbe
import akka.testkit.TestActor
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import akka.http.scaladsl.marshalling._

object FutureCompleteSpec {
  def route(actor: ActorRef) =
    get {
      implicit val timeout = Timeout(1.second)
      complete((actor ? "req").mapTo[(StatusCode, String)])
    } ~
    post {
      implicit val mapMarshaller: ToEntityMarshaller[String] = Marshaller.opaque { str =>
        HttpEntity(ContentType(MediaTypes.`application/json`), str)
      }
      complete(Future.successful("ohi"))
    }
}

class FutureCompleteSpec extends WordSpec with Matchers with ScalatestRouteTest {
  import FutureCompleteSpec._

  "route" should {
    "return status code and response" in {

      val probe = TestProbe()
      probe.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = msg match {
          case "req" ⇒ sender ! (StatusCodes.Accepted -> "ohi"); TestActor.KeepRunning
        }
      })

      Get() ~> route(probe.ref) ~> check {
        status === StatusCodes.Accepted
        responseAs[String] shouldEqual "ohi"
      }

      Post() ~> route(probe.ref) ~> check {
        status === StatusCodes.Accepted
        responseAs[String] shouldEqual "ohi"
        response.header[`Content-Type`] shouldBe Some(ContentTypes.`application/json`)
      }
    }
  }

}
