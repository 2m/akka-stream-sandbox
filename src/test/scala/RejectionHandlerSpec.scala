import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Host, HttpChallenge}
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsRejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._

object RejectionHandlerSpec {

  def checkHost(predicate: String ⇒ Boolean): Directive0 =
    extractHost.require(predicate, AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("", None)))

  val route =
    get {
      checkHost(_.contains("get")) {
        complete("get")
      }
    } ~
    post {
      checkHost(_.contains("post")) {
        complete("post")
      }
    }
}

class RejectionHandlerSpec extends WordSpec with Matchers with ScalatestRouteTest {
  import RejectionHandlerSpec._

  "route" should {
    "return status code and response" in {

      Get() ~> Host("get.2m.lt") ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldEqual "get"
      }

      Post() ~> Host("get.2m.lt") ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] shouldEqual "The supplied authentication is invalid"
      }

      Post() ~> Host("post.2m.lt") ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldEqual "post"
      }

      Post() ~> Host("get.2m.lt") ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] shouldEqual "The supplied authentication is invalid"
      }
    }
  }

}
