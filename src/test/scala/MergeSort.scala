import akka.actor.ActorSystem
import akka.stream._
import akka.stream.FanInShape._
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.WordSpec

object MergeSort {

  class LeftRightPorts[A](_init: Init[A] = Name("LeftRight")) extends FanInShape[A](_init) {
    val left = newInlet[A]("left")
    val right = newInlet[A]("right")
    protected override def construct(i: Init[A]) = new LeftRightPorts(i)
  }

  class LeftRightMerge[A] extends FlexiMerge[A, LeftRightPorts[A]](new LeftRightPorts, Attributes.name("LeftRightMerge")) {
    import FlexiMerge._

    override def createMergeLogic(p: PortT) = new MergeLogic[A] {
      val readLeft: State[A] = State[A](Read(p.left)) { (ctx, input, element) =>
        ctx.emit(element)
        readRight
      }

      val readRight: State[A] = State[A](Read(p.right)) { (ctx, input, element) =>
        ctx.emit(element)
        readLeft
      }

      override def initialState: State[_] = readLeft
    }
  }

  class Sorter[A: Ordering] extends PushPullStage[A, A] {
    private final val StoreSize = 2
    private var store = List.empty[A]
    private val ord = implicitly[Ordering[A]]

    override def onPush(elem: A, ctx: Context[A]): SyncDirective =
      if (store.size < StoreSize) {
        store = storeInOrder(elem, store)
        ctx.pull()
      } else {
        val (toPush :: toStore) = storeInOrder(elem, store)
        store = toStore
        ctx.push(toPush)
      }

    override def onPull(ctx: Context[A]): SyncDirective = store match {
      case n :: Nil if ctx.isFinishing =>
        ctx.pushAndFinish(n)
      case n :: tail if ctx.isFinishing =>
        store = tail
        ctx.push(n)
      case _ =>
        ctx.pull()
    }

    override def onUpstreamFinish(ctx: Context[A]): TerminationDirective = store match {
      case Nil => ctx.finish()
      case _ => ctx.absorbTermination()
    }

    private def storeInOrder(elem: A, store: List[A]): List[A] = store match {
      case Nil => elem :: Nil
      case n :: tail if ord.lt(n, elem) => n :: storeInOrder(elem, tail)
      case n :: tail if ord.gt(n, elem) => elem :: n :: tail
    }

  }

  val mergeSortGraph = FlowGraph.partial() { implicit b =>
    import FlowGraph.Implicits._

    val merge = b.add(new LeftRightMerge[Int])
    val out = merge.out.transform { () => new Sorter() }.outlet
    UniformFanInShape(out, merge.left, merge.right)
  }
}

class MergeSort extends WordSpec {
  import MergeSort._

  implicit val sys = ActorSystem("MergeSort")
  implicit val mat = ActorMaterializer()

  "merge sort" should {

    def mergeSort(s1: List[Int], s2: List[Int], expected: List[Int]) = {
      val probe = FlowGraph.closed(TestSink.probe[Int]) { implicit b => sink =>
        import FlowGraph.Implicits._
        val ms = b.add(mergeSortGraph)
        Source(s1) ~> ms.in(0)
        Source(s2) ~> ms.in(1)
        ms.out ~> sink
      }.run()

      probe.request(expected.size)
      probe.expectNext(expected.head, expected.tail.head, expected.tail.tail :_*)
      probe.expectComplete()
    }

    "sort in order" in {
      mergeSort(List(1, 2, 4, 10), List(3, 5, 8, 9), List(1, 2, 3, 4, 5, 8, 9, 10))
    }

    "sort in order 2" in {
      mergeSort(List(1, 2, 9, 10), List(3, 4, 5, 7), List(1, 2, 3, 4, 5, 7, 9, 10))
    }
  }

}
