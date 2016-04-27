import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{ DefaultFormats, jackson }
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import scala.concurrent.ExecutionContext
import akka.util.ByteString

object Client extends Json4sSupport {

  def request(path: String, host: String, port: Int)(implicit sys: ActorSystem, mat: Materializer, ec: ExecutionContext) = {

    implicit val serialization = jackson.Serialization // or native.Serialization
    implicit val formats = DefaultFormats

    val data = List(
      render(Map(
        "index" -> Map(
          "_index" -> "test",
          "_type" -> "type1",
          "_id" -> "1"
        )
      )),
      render(
        Map(
          "field1" -> "value1"
        )
      )
    )

    val dataSource = Source(data)
      .map(compact(_))
      .intersperse("\n")
      .map(ByteString.apply)

    Source
      .single(HttpRequest(POST, Uri.from(path = path), entity = HttpEntity(ContentTypes.`application/json`, dataSource)))
      .via(Http().outgoingConnection(host, port))
      .runWith(Sink.head)
      .flatMap(Unmarshal(_).to[JValue])
  }

}
