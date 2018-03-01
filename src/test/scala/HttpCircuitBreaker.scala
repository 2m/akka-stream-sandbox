import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.pattern.CircuitBreaker
import akka.stream.ActorMaterializer
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}

object HttpCircuitBreakerSpec {
  import akka.http.scaladsl.server.Directives._

  def route(cb: CircuitBreaker) =
    path("test") {
      val never = Promise[String]
      onCompleteWithBreaker(cb)(never.future) {
        case Success(t) => complete(t)
        case Failure(ex) => complete(ex.getMessage)
      }
    }
}

class HttpCircuitBreakerSpec extends WordSpec with ScalaFutures {
  import HttpCircuitBreakerSpec._

  "circuit breaker" should {
    "timeout" in {
      implicit val sys = ActorSystem()
      implicit val mat = ActorMaterializer()

      val breaker = CircuitBreaker(sys.scheduler, 2, 2.seconds, 5.seconds)

      val binding = Http().bindAndHandle(route(breaker), "127.0.0.1", 8080)

      StdIn.readLine()

      binding.futureValue.unbind()
      sys.terminate().futureValue
    }
  }



}
