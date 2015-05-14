import org.scalatest.WordSpec
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent._
import scala.concurrent.duration._

class ResponseAsString extends WordSpec {

  "response" should {
    "be parsed as a string" in {
      implicit val system = ActorSystem("client-system")
      implicit val materializer = ActorFlowMaterializer()
      import system.dispatcher

      val host = "127.0.0.1"
      val httpClient = Http(system).outgoingConnection(host, 9999)

      def get(url: String): Future[String] =
        Source.single(HttpRequest(uri = Uri(url))).via(httpClient).mapAsync(parallelism=4)(Unmarshal(_).to[String]).runWith(Sink.head)

      println(Await.result(get("/"), 2 seconds))
    }
  }

}
