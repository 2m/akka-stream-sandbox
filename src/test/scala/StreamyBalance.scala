import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._
import akka.stream._
import org.scalatest._
import akka.actor._
import akka.stream.FanOutShape._

object StreamyBalance {

  object TheFlexiRoute {
    trait RouteMessage
    case object Single extends RouteMessage
    case object Broadcast extends RouteMessage

    def apply(outs: Int) = {
      val shape = new UniformFanOutShape[TheFlexiRoute.RouteMessage, TheFlexiRoute.RouteMessage](outs, Name[TheFlexiRoute.RouteMessage]("StreamyBalance"))
      new TheFlexiRoute(shape, Attributes.name("streamyBalance"))
    }
  }

  class TheFlexiRoute(shape: UniformFanOutShape[TheFlexiRoute.RouteMessage, TheFlexiRoute.RouteMessage], attributes: Attributes) extends FlexiRoute[TheFlexiRoute.RouteMessage, UniformFanOutShape[TheFlexiRoute.RouteMessage, TheFlexiRoute.RouteMessage]](shape, attributes) {
    import FlexiRoute._
    override def createRouteLogic(p: PortT) = new RouteLogic[TheFlexiRoute.RouteMessage] {

      val broadcastState = {
        State(DemandFromAll(p.outlets)) {
          (ctx, _, element) =>
            element match {
              case TheFlexiRoute.Broadcast =>
                p.outArray.foreach{ o =>
                  ctx.emit(o)(element)
                }
                SameState
              case TheFlexiRoute.Single =>
                normalState
            }
        }
      }

      val normalState:State[OutPort] = {
        State(DemandFromAny(p.outlets)) {
          (ctx, out, element) =>
            element match {
              case TheFlexiRoute.Broadcast =>
                broadcastState

              case TheFlexiRoute.Single =>
                ctx.emit(out.asInstanceOf[Outlet[TheFlexiRoute.RouteMessage]])(element)
                SameState
            }
        }
      }

      override def initialState = normalState
    }
  }
}

class StreamyBalance extends WordSpec with Matchers {

  import StreamyBalance._
  import StreamyBalance.TheFlexiRoute._

  implicit val sys = ActorSystem("StreamyBalance")
  implicit val mat = ActorMaterializer()

  "TheFlexiRoute" should {
    "Route messages correctly based on type" in {

      val testSink = Sink.foreach[RouteMessage]{ m =>
        println(s"Sink gets: $m")
      }

      val flow = Flow() { implicit b =>
        import FlowGraph.Implicits._
        val router = b.add(TheFlexiRoute(2))
        val merge = b.add(Merge[TheFlexiRoute.RouteMessage](2))

        router.out(0) ~> merge.in(0)
        router.out(1) ~> merge.in(1)

        (router.in, merge.out)
      }

      val (pub, sub) = TestSource.probe[RouteMessage]
        .via(flow)
        .toMat(TestSink.probe[RouteMessage])(Keep.both)
        .run()

      sub.request(3)

      pub.sendNext(Single)
      sub.expectNext(Single)

      pub.sendNext(Broadcast)
      sub.expectNoMsg()

      pub.sendNext(Broadcast)
      sub.expectNext(Broadcast)
      sub.expectNext(Broadcast)

      pub.sendComplete()
      sub.expectComplete()
    }
  }

}
