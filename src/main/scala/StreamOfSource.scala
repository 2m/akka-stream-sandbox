import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

object StreamOfSource extends App {

  implicit val sys = ActorSystem()
  implicit val mat = ActorMaterializer()

  Source(1 to 10)
    .map(i => Source(1 to i))
    .flatMapConcat(identity)
    .runForeach(print)
}
