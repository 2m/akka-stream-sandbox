package scan

import org.scalatest.WordSpec
import akka.stream.scaladsl.Source
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

class ScanSanbox extends WordSpec {

  implicit val sys = ActorSystem()
  implicit val mat = ActorMaterializer()

  "scan" should {
    "scan" in {
      Source(0 to 5).scan(List[Int]()) {
        case (list, element) => element :: list
      }.runWith(Sink.foreach(println))
    }
  }

}
