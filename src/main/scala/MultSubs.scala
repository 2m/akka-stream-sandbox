import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream._

object MultSubs extends App {

  /*implicit val sys = ActorSystem("kuku")
  implicit val mat = ActorFlowMaterializer()

  val in1 = Source(List("a"))
  val in2 = Source(List("b"))

  val p1 = Sink.fanoutPublisher[String](8, 8)
  val p2 = Sink.fanoutPublisher[String](8, 8)

  val out1 = Sink.foreach(println)
  val out2 = Sink.foreach(println)

  val graph = FlowGraph { implicit b ⇒
    import FlowGraphImplicits._

    val bcast = Broadcast[String]
    val merge = Merge[String]

    in1 ~> Flow[String] ~> merge
    in2 ~> Flow[String] ~> merge
    merge ~> Flow[String] ~> bcast
    bcast ~> Flow[String] ~> p1
    bcast ~> Flow[String] ~> p2
  }.run()

  Source(graph.get(p1)).map(_ + "qqq").runWith(out1)
  Source(graph.get(p2)).map(_ + "qqq").runWith(out2)*/

}
