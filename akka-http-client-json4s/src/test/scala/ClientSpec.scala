import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.stream.Materializer
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.Http.ServerBinding
import scala.concurrent.Promise
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.Matchers
import com.typesafe.config.ConfigFactory
import org.scalatest.time._
import org.json4s.{ DefaultFormats, jackson }
import org.json4s.JsonAST._
import akka.util.ByteString
import akka.http.scaladsl.model._

class ClientSpec extends WordSpec with Directives with Matchers with ScalaFutures {
  import Client._

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  "client" should {
    "send request and parse response" in withFixture { binding => sentReqeust => implicit sys => implicit mat => implicit ec =>
      val response = request("/_bulk", binding.localAddress.getHostString, binding.localAddress.getPort)

      response.futureValue \ "took" shouldBe JInt(7)
      sentReqeust.futureValue.utf8String shouldBe """|{"index":{"_index":"test","_type":"type1","_id":"1"}}
                                                     |{"field1":"value1"}""".stripMargin
    }
  }

  def withFixture(test: ServerBinding => Future[ByteString] => ActorSystem => Materializer => ExecutionContext => Any) {
    val conf = ConfigFactory.parseString("""
      akka.loglevel = debug
    """).withFallback(ConfigFactory.load)
    implicit val sys = ActorSystem("ClientSpec", conf)
    implicit val mat = ActorMaterializer()

    val requestPromise = Promise[ByteString]

    val server = Http().bindAndHandle(
      post {
        path("_bulk") {
          entity(as[ByteString]) { request =>
            requestPromise.success(request)
            complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"took":7,"items":[{"create":{"_index":"test","_type":"type1","_id":"1","_version":1}}]}""")))
          }
        }
      }, "localhost", 0)
    val binding = Await.result(server, 2.seconds)

    try {
      test(binding)(requestPromise.future)(sys)(mat)(sys.dispatcher)
    } finally {
      Await.ready(binding.unbind(), 2.seconds)
      Await.ready(sys.terminate(), 2.seconds)
    }
  }
}
