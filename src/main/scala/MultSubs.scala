import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream._

object MultSubs extends App {

  implicit val sys = ActorSystem("kuku")
  implicit val mat = ActorFlowMaterializer()

  val in1 = Source(List("a"))
  val in2 = Source(List("b"))

  val out1 = Sink.foreach(println)
  val out2 = Sink.foreach(println)

  val (p1, p2) = FlowGraph.closed(Sink.fanoutPublisher[String](8, 8), Sink.fanoutPublisher[String](8, 8))(Keep.both) { implicit b ⇒ (p1, p2) =>
    import FlowGraph.Implicits._

    val bcast = b.add(Broadcast[String](outputPorts = 2))
    val merge = b.add(Merge[String](inputPorts = 2))

    in1 ~> Flow[String] ~> merge
    in2 ~> Flow[String] ~> merge
    merge ~> Flow[String] ~> bcast
    bcast ~> Flow[String] ~> p1
    bcast ~> Flow[String] ~> p2
  }.run()

  Source(p1).map(_ + "qqq").runWith(out1)
  Source(p2).map(_ + "qqq").runWith(out2)

}
