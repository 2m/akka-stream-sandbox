import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._
import akka.stream.stage._
import com.typesafe.config.ConfigFactory
import Util._
import scala.concurrent.Await
import scala.concurrent.duration._

object CustomFanOut2 extends App {

  case class Course(kmsToDestination: Integer)
  case class Plane(course: Course)
  case class Landing(plane: Plane)

  final class CourseCompleter extends GraphStage[FanOutShape2[Plane, Plane, Landing]] {
    override def initialAttributes = Attributes.name("CourseCompleter")

    val in = Inlet[Plane]("InboundCourse")
    val courseOut = Outlet[Plane]("IncompleteCourse")
    val landingOut = Outlet[Landing]("Landing")

    val shape: FanOutShape2[Plane, Plane, Landing] = new FanOutShape2(in, courseOut, landingOut)

    def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

      var planeInFlight = Option.empty[Plane]

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val plane = grab(in)
          if (plane.course.kmsToDestination <= 0 && isAvailable(landingOut)) {
            push(landingOut, Landing(plane))
          } else if (plane.course.kmsToDestination > 0 && isAvailable(courseOut)) {
            push(courseOut, plane)
          }
          else {
            // got a plane, but nowhere to go, because demand has not been signalled
            // store it until demand comes
            planeInFlight = Some(plane)
          }
        }

        override def onUpstreamFinish() = completeStage()
      })

      setHandler(courseOut, new OutHandler {
        override def onPull(): Unit = planeInFlight match {
          case Some(plane) if plane.course.kmsToDestination > 0 =>
            push(courseOut, plane)
            planeInFlight = None
          case None =>
            if (!hasBeenPulled(in)) tryPull(in)
          case _ =>
            // one plane is already stored, but this outlet can not accept it
            // do nothing
        }
      })

      setHandler(landingOut, new OutHandler {
        override def onPull(): Unit = planeInFlight match {
          case Some(plane) if plane.course.kmsToDestination <= 0 =>
            push(landingOut, Landing(plane))
            planeInFlight = None
          case None =>
            if (!hasBeenPulled(in)) tryPull(in)
          case _ =>
            // one plane is already stored, but this outlet can not accept it
            // do nothing
        }
      })
    }
  }

  val cfg = ConfigFactory.parseString("""
    akka {
      loglevel = "DEBUG"
    }
  """).withFallback(ConfigFactory.load())

  implicit val sys = ActorSystem("CustomFanOut2", cfg)
  implicit val mat = ActorMaterializer()

  val graph = RunnableGraph.fromGraph {
    GraphDSL.create(Source.actorRef[Plane](8, OverflowStrategy.fail), TestSink.probe[Plane], TestSink.probe[Landing])((_, _, _)) { implicit builder =>
      (source, planes, landings) =>
        import GraphDSL.Implicits._

        val cmp = builder.add(new CourseCompleter())
        source ~> cmp.in
        cmp.out0.log("Flyby:", identity) ~> planes
        cmp.out1.log("Landing:", identity) ~> landings

        ClosedShape
    }
  }

  val (planeRef, planes, landings) = graph.run()

  repl {
    case Int(kms) => planeRef ! Plane(Course(kms)); Some(s"Sending a plane with $kms km to destination.")
    case r"p:(\d+)$planeDemand" => planes.request(planeDemand.toInt); Some(s"Requested $planeDemand planes.")
    case r"l:(\d+)$landingDemand" => landings.request(landingDemand.toInt); Some(s"Requested $landingDemand landings.")
    case "q" => None
  }

  Await.ready(sys.terminate(), 10.seconds)
}
