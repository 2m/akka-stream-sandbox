import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{BidiFlow, Flow, Keep, Source, SourceQueueWithComplete, Tcp}
import akka.util.ByteString

import scala.concurrent.{Future, Promise}

object MqttBidi {

  implicit val sys: ActorSystem = ???
  implicit val mat: Materializer = ???
  import sys.dispatcher

  trait Mqtt {
    def toBytes: ByteString = ???
  }
  object Mqtt {
    def fromBytes(bytes: ByteString): Mqtt  = ???

    case class Connect(user: String, pass: String) extends Mqtt
    case object ConnectAck extends Mqtt

    case class Subscribe(topic: String) extends Mqtt
    case object SubscribeAck extends Mqtt

    case class Publish(topic: String, data: ByteString) extends Mqtt
    case class PubRec(topic: String, data: ByteString) extends Mqtt
  }

  val queuePromise = Promise[SourceQueueWithComplete[Mqtt]]

  val clientToServerFlow: Flow[Mqtt, ByteString, NotUsed] =
    Flow[Mqtt]
      .concat(Source.single[Mqtt](Mqtt.Connect("someuser", "somepassword")))
      .concatMat(Source.queue[Mqtt](2, OverflowStrategy.backpressure))(Keep.right)
      .map(_.toBytes)
      .mapMaterializedValue { queue =>
        queuePromise.success(queue)
        NotUsed
      }

  val serverToClientFlow: Flow[ByteString, Mqtt, NotUsed] =
    Flow[ByteString]
      .map(Mqtt.fromBytes)
      .mapAsync(1) {
        case msg @ Mqtt.ConnectAck => queuePromise.future.flatMap(_.offer(Mqtt.Subscribe("topic"))).map(_ => msg)
        case msg @ Mqtt.SubscribeAck => queuePromise.future.flatMap(_.offer(Mqtt.Publish("topic", ByteString("hi")))).map(_ => msg)
        case msg => Future.successful(msg)
      }

  val tcpTransport = Tcp().outgoingConnection("localhost", 1883)
  val mqttProtocol = BidiFlow.fromFlows(clientToServerFlow, serverToClientFlow)
  val clientHandler = Flow[Mqtt].wireTap(println(_))

  val program = clientHandler
    .join(mqttProtocol)
    .join(tcpTransport)
}
