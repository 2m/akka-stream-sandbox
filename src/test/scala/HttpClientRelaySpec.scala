import org.scalatest.WordSpec
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.Await
import akka.stream.stage._
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import scala.concurrent.Future
import scala.util.Try
import scala.util.Success
import akka.stream.Supervision
import akka.stream.ActorMaterializerSettings
import akka.stream.StreamTcpException
import scala.util.Random

class HttpClientRelaySpec extends WordSpec with Directives {

  final val ContentLength = 100000
  final val ContentStream = Source.single(ContentLength).transform { () ⇒
    new PushPullStage[Int, ByteString] {
      var numOfElements = Option.empty[Int]

      private def nextElem = ByteString(scala.util.Random.nextInt().toByte)

      final override def onPush(elem: Int, ctx: Context[ByteString]): SyncDirective = numOfElements match {
        case None ⇒
          numOfElements = Some(elem - 1)
          ctx.push(nextElem)
        case Some(_) ⇒ ctx.fail(new Error("one element already received"))
      }

      final override def onPull(ctx: Context[ByteString]): SyncDirective = numOfElements match {
        case Some(left) if left > 1 ⇒
          numOfElements = Some(left - 1)
          ctx.push(nextElem)
        case Some(left) if left == 1 ⇒
          numOfElements = Some(left - 1)
          ctx.pushAndFinish(nextElem)
        case None ⇒ ctx.pull()
      }

      override def onUpstreamFinish(ctx: Context[ByteString]): TerminationDirective = ctx.absorbTermination()
    }
  }

  "client" should {

    implicit val sys = ActorSystem("ClientResponseConsumeSpec", ConfigFactory.parseString("""
        akka.loglevel = "DEBUG"
        akka.stream.materializer.subscription-timeout.timeout = 1m
        akka.http.client.connecting-timeout = 30s
        akka.http.host-connection-pool.max-connections = 16
      """).withFallback(ConfigFactory.load))
    implicit val mat = ActorMaterializer()
    import sys.dispatcher

    "server" in {
      val binding = Await.result(Http().bindAndHandle(
        path("forward") {
          complete(HttpEntity(ContentTypes.`application/octet-stream`, ContentLength, ContentStream))
        } ~
        path("forward2") {
          extractRequest { r ⇒
            val count = r.entity.dataBytes.runFold(0) { case (acc, bs) ⇒ acc + bs.length }
            import sys.dispatcher
            count.onComplete { x ⇒ println(s"Completed reading request with $x") }
            complete("ok")
          }
        }, "127.0.0.1", 52214), 1.second)

      scala.io.StdIn.readLine()

      Await.ready(Http().shutdownAllConnectionPools(), 1.second)
      Await.ready(binding.unbind(), 1.second)
      sys.shutdown()
      sys.awaitTermination()
    }

    "relay" in {
      val hostname = "127.0.0.1"
      val port = 52214


      Source.repeat("tick").mapAsyncUnordered(4) { _ =>
        clientRelayPool(hostname, port, "forward", "forward2")
      //}.runForeach(x => println(s"Response $x"))
      }.runFold((0f, System.currentTimeMillis)) { case ((count, start), response) =>
        val totalSecs = (System.currentTimeMillis - start) / 1000
        println(s"New response: $response. Total $count responses in ${totalSecs}s, avg: ${count / totalSecs}")
        (count + 1, start)
      }

      scala.io.StdIn.readLine()
    }
  }

  def clientRelay(host: String, port: Int, path: String, path2: String)(implicit sys: ActorSystem, mat: ActorMaterializer) = {
    import sys.dispatcher
    Http().singleRequest(HttpRequest(uri = s"http://$host:$port/$path")).flatMap { response ⇒
      Http().singleRequest(HttpRequest(method = HttpMethods.PUT, uri = s"http://$host:${port + 1000}/$path2", entity = response.entity.asInstanceOf[UniversalEntity])).flatMap { r ⇒
        Unmarshal(r.entity).to[String]
      }
    }
  }

  def clientRelayPool(hostname: String, port: Int, path: String, path2: String)(implicit sys: ActorSystem, mat: ActorMaterializer) = {
    import sys.dispatcher
    val pool = Http().newHostConnectionPool[Int](hostname, port)
    val request = HttpRequest(uri = s"http://$hostname:$port/$path")
    Source.single(request -> 42).via(pool).runWith(Sink.head).flatMap {
      case (Success(r), id) ⇒ Unmarshal(r.entity).to[String].map(_.length)
      case _ => println("### ERROR"); Future.failed(new Error("failed"))
    }
    /*Http().singleRequest(r).flatMap { response ⇒
      val offset = if (Random.nextBoolean()) 1000 else 0
      val request = HttpRequest(method = HttpMethods.PUT, uri = s"http://$host:${port + offset}/$path2", entity = response.entity.asInstanceOf[UniversalEntity])
      val decider: Supervision.Decider = {
        case _: StreamTcpException => println("ex"); Supervision.Resume
        case _                      => Supervision.Stop
      }
      val mate = ActorMaterializer(ActorMaterializerSettings(sys).withSupervisionStrategy(decider))
      Source.single(request -> 42).via(pool).runWith(Sink.head)(mate).flatMap { case (Success(r), id) ⇒
        Unmarshal(r.entity).to[String]
      }
    }*/
  }

}
