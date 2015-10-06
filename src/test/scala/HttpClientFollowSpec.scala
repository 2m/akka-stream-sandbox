import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.Http
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future
import org.scalatest.Matchers
import org.scalatest.fixture

import java.net.InetSocketAddress

class HttpClientFollowSpec extends fixture.WordSpec with Directives with Matchers {

  final val Data = "hello"
  final val ContentLength = Data.size
  final val ContentStream = Source.single(ByteString(Data))

  case class FixtureParam(localAddress: InetSocketAddress, implicit val sys: ActorSystem, implicit val mat: ActorMaterializer)

  def withFixture(test: OneArgTest) = {

    implicit val sys = ActorSystem("HttpClientFollowSpec")
    implicit val mat = ActorMaterializer()

    val binding = Await.result(Http().bindAndHandle(
      path("resource2") {
        complete(HttpEntity(ContentTypes.`application/octet-stream`, ContentLength, ContentStream))
      } ~
      path("resource") {
        redirect("/resource2", StatusCodes.PermanentRedirect)
      } ~
      path("loop") {
        redirect("/loop", StatusCodes.PermanentRedirect)
      }, "127.0.0.1", 0), 1.second)

    try {
      withFixture(test.toNoArgTest(FixtureParam(binding.localAddress, sys, mat))) // "loan" the fixture to the test
    }
    finally {
      Await.ready(Http().shutdownAllConnectionPools(), 1.second)
      Await.ready(binding.unbind(), 1.second)
      sys.shutdown()
      sys.awaitTermination()
    }
  }

  "client" should {

    "follow redirect" in { f =>
      import f._
      val resp = Await.result(clientFollow(f.localAddress.getHostString, f.localAddress.getPort, "resource"), 1.second)
      resp shouldBe Data.toString
    }

    "have finite follow attempts" in { f =>
      import f._
      val resp = Await.result(clientFollow(f.localAddress.getHostString, f.localAddress.getPort, "loop").failed, 1.second)
      resp.getCause.getMessage shouldBe "Too many redirects"
    }
  }

  def clientFollow(host: String, port: Int, path: String)(implicit sys: ActorSystem, mat: ActorMaterializer) = {
    import sys.dispatcher

    def withFollow(uri: Uri, depth: Int = 5): Future[String] = depth match {
      case d if d <= 0 => Future.failed(new Error("Too many redirects"))
      case _ =>
        Http().singleRequest(HttpRequest(uri = uri.withAuthority(host, port).withScheme("http"))).flatMap { response ⇒
          response.status match {
            case StatusCodes.OK => Unmarshal(response.entity).to[String]
            case StatusCodes.PermanentRedirect =>
              response.entity.dataBytes.runWith(Sink.ignore)
              response.header[Location]
                .map { l => withFollow(l.uri, depth - 1) }
                .getOrElse(Future.failed(new Error("No location in redirect")))
            case status =>
              response.entity.dataBytes.runWith(Sink.ignore)
              Future.failed(new Error(s"Unsupported status code: status"))
          }
        }
    }

    withFollow(s"/$path")
  }
}
