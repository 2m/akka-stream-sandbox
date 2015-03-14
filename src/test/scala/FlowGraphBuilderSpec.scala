package akka.stream.scaladsl

import akka.actor._
import akka.stream.ActorFlowMaterializer
import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalatest.WordSpec
import org.scalatest.Matchers

class FlowGraphBuilderSpec extends WordSpec with Matchers {

  /*implicit val sys = ActorSystem()
  implicit val mat = ActorFlowMaterializer()

  val inA = UndefinedSource[String]
  val outA = UndefinedSink[Int]

  val graphA = PartialFlowGraph { implicit b =>
    import FlowGraphImplicits._
    inA ~> Flow.apply[String].map(_.length) ~> outA
  }

  val inB = UndefinedSource[Int]
  val outB = UndefinedSink[Char]

  val graphB = PartialFlowGraph { implicit b =>
    import FlowGraphImplicits._
    inB ~> Flow.apply[Int].map(i => (i+48).toChar) ~> outB
  }

  // graph C is not compatible with the output of graph A
  val inC = UndefinedSource[Char]
  val outC = UndefinedSink[Char]

  val graphC = PartialFlowGraph { implicit b =>
    import FlowGraphImplicits._
    inC ~> Flow.apply[Char] ~> outC
  }


  "two incompatible partial graphs" must {
    "not be allowed to connect" in {

      val source = Source(List("hello", "world"))
      val sink = Sink.fold[String, Char]("")((str, c) => str + c)

      val combined = FlowGraph { implicit b =>

        b.importPartialFlowGraph(graphA)
        b.importPartialFlowGraph(graphC)

        // this should not be allowed to compile?
        b.connect(outA, Flow.apply, inC)

        b.attachSource(inA, source)
        b.attachSink(outC, sink)
      }

      val string = Await.result(combined.run().get(sink), 5.seconds)
      string should be("55")
    }
  }


  "two partial graphs" must {
    "be connected in a type-safe way" in {

      val source = Source(List("hello", "world"))
      val sink = Sink.fold[String, Char]("")((str, c) => str + c)

      val combined = FlowGraph { implicit b =>

        b.importPartialFlowGraph(graphA)
        b.importPartialFlowGraph(graphB)

        //b.connect(outA, Flow.apply, inB) // compiles, runs
        //b.connect[Int, Int](outA, Flow.apply, inB) //compiles, runs
        b.connect(outA, Flow.apply[String], inB) //compiles, does not run
        //b.connect[Int, Int](outA, Flow.apply[Double].map(x=>x*2), inB) //does not compile

        b.attachSource(inA, source)
        b.attachSink(outB, sink)

      }

      val string = Await.result(combined.run().get(sink), 5.seconds)
      string should be("55")

    }
  }*/

}
