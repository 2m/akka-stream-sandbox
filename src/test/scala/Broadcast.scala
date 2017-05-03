import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, AmorphousShape}
import akka.stream.scaladsl._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{Future, Promise}

class Broadcast extends WordSpec with Matchers with ScalaFutures {

  implicit val sys = ActorSystem("Broadcast")
  implicit val mat = ActorMaterializer()

  "broadcast" should {
    "stream to both with a hub" in {

      val goPromise = Promise[Done]

      def waitForGo[T](goPromise: Promise[Done])(el: T): Future[T] = {
        if (goPromise.isCompleted) {
          Future.successful(el)
        } else {
          val p = Promise[T]
          import scala.concurrent.ExecutionContext.Implicits.global
          goPromise.future.onComplete(t => p.tryComplete(t.map(_ => el)))
          p.future
        }
      }

      val r = 1 to 10

      val source = Source(r).mapAsync(1)(waitForGo(goPromise))//.concatMat(Source.maybe)(Keep.right)
      val (completion, producer) = source.toMat(BroadcastHub.sink(bufferSize = 16))(Keep.both).run()

      //completion.success(None)


      val f1 = producer.mapAsync(1)(waitForGo(goPromise)).runWith(Sink.seq)
      val f2 = producer.mapAsync(1)(waitForGo(goPromise)).runWith(Sink.seq)

      goPromise.success(Done)

      f1.futureValue should contain theSameElementsInOrderAs (r) // consumes all elements
      f2.futureValue should contain theSameElementsInOrderAs (r) // before this one subscribes


    }

    "stream to both with a graph" ignore {

      val source = Source(1 to 10)

      val graph = GraphDSL.create(source) {
        implicit b => s =>
          import GraphDSL.Implicits._

          val bcast = b.add(Broadcast[Int](outputPorts = 2))
          s ~> bcast.in

          AmorphousShape(Nil, List(bcast.out(0), bcast.out(1)))
      }

      //graph.

    }
  }

}
