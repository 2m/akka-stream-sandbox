import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.stream.scaladsl._

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object ActorRefCombine {
  def main(args: Array[String]) = {
    val BufferSize = 8

    def source1 = Source.actorRef[Int](BufferSize, OverflowStrategy.fail)
    def source2 = Source.actorRef[Int](BufferSize, OverflowStrategy.fail)
    def source3 = Source.actorRef[Int](BufferSize, OverflowStrategy.fail)

    val g = RunnableGraph.fromGraph(GraphDSL.create(source1, source2, source3)((_, _, _)) { implicit builder => (s1, s2, s3) =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](3))
      val out = Sink.foreach(println)

      s1 ~> merge
      s2 ~> merge ~> out
      s3 ~> merge

      ClosedShape
    })

    implicit val sys = ActorSystem()
    implicit val mat = ActorMaterializer()

    val (ref1, ref2, ref3) = g.run()

    ref1 ! 1
    ref2 ! 2
    ref3 ! 3

    ref1 ! 4

    StdIn.readLine()
    sys.terminate()
  }
}

object Reservation {
  def partial(ref: ActorRef) = Flow[(Int, String, String)]
}

object OnBoardPackage {

  protected def partial(nclApiService: ActorRef)(dbActor: ActorRef)(sailingID: Int, shipCode: String, startDate: String)(implicit timeout: akka.util.Timeout, system: akka.actor.ActorSystem, materializer: akka.stream.Materializer, executionContext: ExecutionContext) = GraphDSL.create() {
    implicit builder =>

      //import Graphs.Reservation._
      //import OnboardHelper.SQL._

      //        val reservationSource = Source.single((sailingID, shipCode, startDate)).via(Reservation.partial(nclApiService))

      println(s"SOURCING... $sailingID $shipCode $startDate")

      def cruiseRetrieveBookings(ref: ActorRef) = Flow[(Int, String, String)]
      def partialItineraryGraph(ref: ActorRef)(ref2: ActorRef) = Flow[(Int, String, String)]
      def jsValueToSqlBlob(s: String)(js: (Int, String, String))(s2: String): String = ???

      val itinerarySource = Source.lazily(() =>
        Source.single((sailingID, shipCode, startDate))
          .via(cruiseRetrieveBookings(nclApiService))
          .via(partialItineraryGraph(nclApiService)(dbActor))
          .map(jsValue => jsValueToSqlBlob("ncl_onboard_EndecaItineraries")(jsValue)("p_Itinerary_Code"))
      )

      val ONBOARD_ITINERARY_TABLE_BEFORE: String = ???
      val ONBOARD_ITINERARY_TABLE_AFTER: String = ???
      val RESERVATION_TABLE: String = ???
      val USER_TABLE: String = ???

      val singletonSources = builder.add(
        Source.combine(
          //ENDECA ITINERARY
          Source.lazily(() =>
            Source.single(ONBOARD_ITINERARY_TABLE_BEFORE),
          ),
          Source.lazily(() => itinerarySource),
          Source.lazily(() => Source.single(ONBOARD_ITINERARY_TABLE_AFTER)),

          //RES and CLIENT
          Source.lazily(() => Source.single(RESERVATION_TABLE + "\n")),
          Source.lazily(() => Source.single(USER_TABLE + "\n")),
          Source.lazily(() => Source.single((sailingID, shipCode, startDate)).via(Reservation.partial(nclApiService)))


        )(new Concat(_))
      )

      singletonSources
  }.named("onBoardPartial")
  // ...
  // ...

  val apiService: ActorRef = ???
  val dbActor: ActorRef = ???

  val huh = Flow[(Int, String, String)].map(triple => {
    println("HELLO!!!!!!")
    implicit val timeout: akka.util.Timeout = ???
    implicit val system: akka.actor.ActorSystem = ???
    implicit val mat: akka.stream.Materializer = ???
    implicit val ec: scala.concurrent.ExecutionContext = ???
    (Source.fromGraph(OnBoardPackage.partial(apiService)(dbActor: ActorRef)(triple._1, triple._2, triple._3)))
  })
}
