import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import org.scalatest.{BeforeAndAfterAll, WordSpec}

import scala.util.control.NonFatal

object GraphStageRestart {

  class FlowStage extends GraphStage[FlowShape[Int, Int]] {

    val in: Inlet[Int] = Inlet("FlowStage.in")
    val out: Outlet[Int] = Outlet("FlowStage.out")

    override val shape = new FlowShape(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {

        println("In constructor")

        override def preStart(): Unit = {
          println("In preStart")
        }

        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            grab(in) match {
              case 0 => throw new Error("boom!")
              case el => push(out, el)
            }
          }
        })

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        })
      }
  }
}

class GraphStageRestart extends WordSpec with BeforeAndAfterAll {
  import GraphStageRestart._

  implicit val sys = ActorSystem()

  val set = ActorMaterializerSettings(sys).withSupervisionStrategy { (e: Throwable) => Supervision.Restart }
  implicit val mat = ActorMaterializer(set)

  "graph stage" should {
    "restart" in {
      Source(List(1, 1, 0, 2)).via(new FlowStage()).runWith(Sink.foreach(println))
    }
  }

  override def afterAll() = {
    sys.terminate()
  }

}
