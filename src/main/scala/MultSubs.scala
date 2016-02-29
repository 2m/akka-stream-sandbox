import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream._

object MultSubs extends App {

  implicit val sys = ActorSystem("kuku")
  implicit val mat = ActorMaterializer()

  val in1 = Source(List("a"))
  val in2 = Source(List("b"))

  val out1 = Sink.foreach(println)
  val out2 = Sink.foreach(println)

  val (p1, p2) = RunnableGraph.fromGraph(GraphDSL.create(Sink.asPublisher[String](fanout=true), Sink.asPublisher[String](fanout=true))(Keep.both) { implicit b ⇒ (p1, p2) =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[String](outputPorts = 2))
    val merge = b.add(Merge[String](inputPorts = 2))

    in1 ~> Flow[String] ~> merge
    in2 ~> Flow[String] ~> merge
    merge ~> Flow[String] ~> bcast
    bcast ~> Flow[String] ~> p1
    bcast ~> Flow[String] ~> p2

    ClosedShape
  }).run()

  Source.fromPublisher(p1).map(_ + "qqq").runWith(out1)
  Source.fromPublisher(p2).map(_ + "qqq").runWith(out2)

}
