import akka.actor._
import akka.pattern.ask
import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline._
import akka.stream._
import akka.stream.scaladsl._
import akka.util._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util._

object StreamException {

  object Util {
    def repl(main: String => Option[String]) = {
      @annotation.tailrec
      def action(): Unit = Try(main(StdIn.readLine)) match {
        case Success(Some(output)) => println(output); action()
        case Success(None) => // stop repl
        case Failure(ex) => println(s"Unrecognized input: $ex. Type 'q' to quit."); action()
      }

      action()
    }
  }

  class Parent(implicit mat: ActorMaterializer) extends Actor {

    val child = context.actorOf(Props(new Child()), "child")

    var main: ActorRef = _

    def receive = {
      case "stream" =>
        main = sender()
        child ! "stream"
      case ref: ActorRef => main ! ref
    }
  }

  class Child(implicit mat: ActorMaterializer) extends Actor with ActorLogging with ReceivePipeline {

    val stream = Source
      .actorRef[String](8, OverflowStrategy.dropNew)
      .map {
        case "ex" => throw new Exception("boom exception")
        case "err" => throw new Error("boom error")
        case other => other
      }
      .to(Sink.actorRefWithAck(self, "init", "ack", "complete", _.toString))
      .run

    pipelineInner { case msg ⇒ log.info(s"msg: $msg"); Inner(msg) }

    def receive = {
      case "stream" => sender() ! stream
      case msg => sender() ! "ack"
    }
  }

  object Main extends App {

    import Util._

    implicit val sys = ActorSystem("Main")
    implicit val mat = ActorMaterializer()
    implicit val tim = Timeout(1.second)

    val parent = sys.actorOf(Props(new Parent()), "parent")
    val stream = Await.result((parent ? "stream").mapTo[ActorRef], tim.duration)

    repl {
      case "q" => None
      case str => stream ! str; Some(s"sending $str")
    }

    Await.ready(sys.terminate(), 1.second)
  }
}
