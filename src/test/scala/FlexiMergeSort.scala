import akka.actor.ActorSystem
import akka.stream._
import akka.stream.FanInShape._
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.WordSpec
import scala.util.{ Either, Left, Right }
import scala.collection.mutable

object FlexiMergeSortSpec {

  class LeftRightPorts[A](_init: Init[A] = Name("LeftRight")) extends FanInShape[A](_init) {
    val left = newInlet[A]("left")
    val right = newInlet[A]("right")
    protected override def construct(i: Init[A]) = new LeftRightPorts(i)
  }

  class FlexiMergeSort[A: Ordering] extends FlexiMerge[A, LeftRightPorts[A]](new LeftRightPorts, Attributes.name("FlexiMergeSort")) {
    import FlexiMerge._

    override def createMergeLogic(p: PortT) = new MergeLogic[A] {

      private val ord = implicitly[Ordering[A]]

      val initial: State[A] = State[A](Read(p.left)) { (ctx, input, element) =>
        readRight(element)
      }

      def readLeft: A => State[A] = comparator => State[A](Read(p.left)) { (ctx, input, element) =>
        if (ord.lt(element, comparator)) {
          ctx.emit(element)
          SameState
        }
        else {
          ctx.emit(comparator)
          readRight(element)
        }
      }

      def readRight: A => State[A] = comparator => State[A](Read(p.right)) { (ctx, input, element) =>
        if (ord.lt(element, comparator)) {
          ctx.emit(element)
          SameState
        }
        else {
          ctx.emit(comparator)
          readLeft(element)
        }
      }

      override def initialState: State[_] = initial
    }
  }
}

class FlexiMergeSortSpec extends WordSpec {
  import FlexiMergeSortSpec._

  implicit val sys = ActorSystem("FlexiMergeSort")
  implicit val mat = ActorMaterializer()

  "merge sort" should {

    def mergeSort(s1: List[Int], s2: List[Int], expected: List[Int]) = {
      val probe = FlowGraph.closed(TestSink.probe[Int]) { implicit b => sink =>
        import FlowGraph.Implicits._
        val ms = b.add(new FlexiMergeSort[Int])
        Source(s1) ~> ms.left
        Source(s2) ~> ms.right
        ms.out ~> sink
      }.run()

      probe.request(expected.size)
      probe.expectNext(expected.head, expected.tail.head, expected.tail.tail :_*)
      probe.expectComplete()
    }

    "sort in order" when {

      "sources are trivial" in {
        mergeSort(List(1, 2, 4, 10), List(3, 5, 8, 9), List(1, 2, 3, 4, 5, 8, 9, 10))
      }

      "sources have bigger gaps" in {
        mergeSort(List(1, 2, 9, 10), List(3, 4, 5, 7), List(1, 2, 3, 4, 5, 7, 9, 10))
      }

      "first source is shorter" in {
        mergeSort(List(1, 2, 9), List(3, 4, 5, 7), List(1, 2, 3, 4, 5, 7, 9))
      }

      "second source is shorter" in {
        mergeSort(List(1, 2, 9, 10), List(3, 4, 5), List(1, 2, 3, 4, 5, 9, 10))
      }

      "sources have a very big gap" in {
        val s1 = List(1, 100)
        val s2 = (2 to 99).toList
        mergeSort(s1, s2, (s1 ::: s2).sorted)
      }
    }

  }

}
