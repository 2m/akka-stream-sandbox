package streamybalance

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
      val shape = new StreamyBalancePorts[TheFlexiRoute.RouteMessage, TheFlexiRoute.RouteMessage](outs)
      new TheFlexiRoute(outs, shape, Attributes.name("streamyBalance"))
    }
  }

  class StreamyBalancePorts[A, B](n: Int, _init: FanOutShape.Init[A] = Name[A]("StreamyBalance")) extends UniformFanOutShape[A, B](n, _init) {
    val outs = {
      (0 until n).map { i =>
        newOutlet(s"out${i}")
      }.toSeq
    }
    protected override def construct(i: FanOutShape.Init[A]) = new StreamyBalancePorts(n, i)
  }

  class TheFlexiRoute(outs: Int, shape: UniformFanOutShape[TheFlexiRoute.RouteMessage, TheFlexiRoute.RouteMessage], attributes: Attributes) extends FlexiRoute[TheFlexiRoute.RouteMessage, UniformFanOutShape[TheFlexiRoute.RouteMessage, TheFlexiRoute.RouteMessage]](shape, attributes) {
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

  implicit val sys = ActorSystem("StreamyBalance")
  implicit val mat = ActorMaterializer()

  "TheFlexiRoute" should {
    "Route messages correctly based on type" in {

      val src = TestSource.probe[TheFlexiRoute.RouteMessage]

      val testSink = Sink.foreach[TheFlexiRoute.RouteMessage]{ m =>
        println(s"Sink gets: $m")
      }

      def testFlow(i: Int) = Flow.apply[TheFlexiRoute.RouteMessage].map { m =>
        println(s"Messages-$i: ${m}")
        m
      }

      val flow = Flow() { implicit b =>
        import FlowGraph.Implicits._
        val router = b.add(TheFlexiRoute(2))
        //val router = b.add(Broadcast[TheFlexiRoute.RouteMessage](2))
        val merge = b.add(Merge[TheFlexiRoute.RouteMessage](2))

        router.out(0) ~> testFlow(0) ~> merge.in(0)
        router.out(1) ~> testFlow(1) ~> merge.in(1)
        //Source.single(TheFlexiRoute.Single) ~> merge.in(1)

        (router.in, merge.out)
      }

      val runableFlow = src.via(flow).to(testSink).run()


      runableFlow.sendNext(TheFlexiRoute.Single)

      runableFlow.sendNext(TheFlexiRoute.Single)
      runableFlow.sendNext(TheFlexiRoute.Broadcast)
      runableFlow.sendNext(TheFlexiRoute.Broadcast)

      runableFlow.sendNext(TheFlexiRoute.Single)
      runableFlow.sendNext(TheFlexiRoute.Single)
      runableFlow.sendNext(TheFlexiRoute.Broadcast)
      runableFlow.sendNext(TheFlexiRoute.Broadcast)

      runableFlow.sendNext(TheFlexiRoute.Single)
      runableFlow.sendNext(TheFlexiRoute.Single)
      runableFlow.sendNext(TheFlexiRoute.Broadcast)
      runableFlow.sendNext(TheFlexiRoute.Broadcast)

      Thread.sleep(15000L)
      println("sending complete")
      runableFlow.sendComplete()
      Thread.sleep(1000L)
    }
  }

}
