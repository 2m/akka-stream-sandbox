import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util._

object FlowLatch extends App {

  def repl(main: String => Option[String]) = {
    @annotation.tailrec
    def action(): Unit = Try(main(StdIn.readLine)) match {
      case Success(Some(output)) => println(output); action()
      case Success(None) => // stop repl
      case Failure(_) => println("Unrecognized input. Type 'q' to quit."); action()
    }
    action()
  }

  trait Control
  case object Pause extends Control
  case object Play extends Control

  class Latch extends GraphStage[FanInShape2[Int, Control, Int]] {

    val c: Inlet[Control] = Inlet("LatchControl")

    val in: Inlet[Int] = Inlet("LatchSource")
    val out: Outlet[Int] = Outlet("LatchSink")

    override val shape = new FanInShape2(in, c, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {

        private var latch = true

        setHandler(c, new InHandler {
          override def onPush(): Unit = {
            grab(c) match {
              case Pause => latch = false
              case Play => latch = true; pull(in)
            }
            pull(c)
          }
        })

        setHandler(in, new InHandler {
          override def onPush(): Unit = emit(out, grab(in))
        })

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            if (latch) {
              pull(in)
            }
            if (!hasBeenPulled(c)) {
              pull(c)
            }
          }
        })
      }
  }

  val controlledFlow = GraphDSL.create(Source.actorRef[Control](1, OverflowStrategy.dropHead)) { implicit builder =>
    controlSource =>
      import GraphDSL.Implicits._
      val latch = builder.add(new Latch)
      controlSource ~> latch.in1
      FlowShape(latch.in0, latch.out)
  }

  implicit val sys = ActorSystem("FlowLatch")
  implicit val mat = ActorMaterializer()

  val control = Source.tick(0.seconds, 1.second, 42).viaMat(controlledFlow)(Keep.right).to(Sink.foreach(println)).run()

  repl {
    case "on" => control ! Play; Some("Continuing flow")
    case "off" => control ! Pause; Some("Pausing flow")
    case "q" => None
  }

  sys.shutdown()
  sys.awaitTermination()
}
