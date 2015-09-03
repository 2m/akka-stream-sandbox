import akka.actor.ActorSystem
import akka.stream._
import akka.stream.FanInShape._
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.WordSpec
import scala.util.{ Either, Left, Right }
import scala.collection.mutable

object DynamicMergeSort {

  class LeftRightPorts[A](_init: Init[Either[A, A]] = Name("LeftRight")) extends FanInShape[Either[A, A]](_init) {
    val left = newInlet[A]("left")
    val right = newInlet[A]("right")
    protected override def construct(i: Init[Either[A, A]]) = new LeftRightPorts(i)
  }

  class LeftRightMarkedMerge[A] extends FlexiMerge[Either[A, A], LeftRightPorts[A]](new LeftRightPorts, Attributes.name("LeftRightMarkedMerge")) {
    import FlexiMerge._

    override def createMergeLogic(p: PortT) = new MergeLogic[Either[A, A]] {
      val readLeft: State[A] = State[A](Read(p.left)) { (ctx, input, element) =>
        ctx.emit(Left(element))
        readRight
      }

      val readRight: State[A] = State[A](Read(p.right)) { (ctx, input, element) =>
        ctx.emit(Right(element))
        readLeft
      }

      override def initialState: State[_] = readLeft

      override def initialCompletionHandling =
        CompletionHandling(
          onUpstreamFinish = (ctx, input) => input match {
            case p.left =>
              ctx.changeCompletionHandling(eagerClose)
              State[A](Read(p.right)) { (ctx, input, element) =>
                ctx.emit(Right(element))
                SameState
              }

            case p.right =>
              ctx.changeCompletionHandling(eagerClose)
              State[A](Read(p.left)) { (ctx, input, element) =>
                ctx.emit(Left(element))
                SameState
              }
          },
          onUpstreamFailure = (ctx, input, cause) => {
            ctx.fail(cause)
            SameState
          }
        )
    }
  }

  class Sorter[A: Ordering] extends PushPullStage[Either[A, A], A] {
    private val ord = implicitly[Ordering[A]]

    private var comparator = Option.empty[A]
    private var currentSide: Either[Unit, Unit] = _
    private var leftStore = mutable.Queue.empty[A]
    private var rightStore = mutable.Queue.empty[A]

    override def onPush(elem: Either[A, A], ctx: Context[A]): SyncDirective = {
      if (comparator.isEmpty) {
        comparator = Some(elem.merge)
        currentSide = if (elem.isLeft) Right() else Left()
        ctx.pull()
      }
      else {
        elem.fold(leftStore.enqueue(_), rightStore.enqueue(_))
        nextToPush().fold[SyncDirective] { ctx.pull() } { ctx.push }
      }
    }

    override def onPull(ctx: Context[A]): SyncDirective = {
      if (ctx.isFinishing)
        nextToPush(ctx.isFinishing).fold[SyncDirective] { ctx.pushAndFinish(comparator.get) } { ctx.push }
      else
        ctx.pull()
    }

    override def onUpstreamFinish(ctx: Context[A]): TerminationDirective =
      if (leftStore.isEmpty && rightStore.isEmpty && comparator.isEmpty)
        ctx.finish()
      else
        ctx.absorbTermination()

    private def nextToPush(takeFromOtherSide: Boolean = false) = {
      val storeToSearch =
        if (currentSide == Right())
          if (rightStore.isEmpty && takeFromOtherSide)
            leftStore
          else
            rightStore
        else
          if (leftStore.isEmpty && takeFromOtherSide)
            rightStore
          else
            leftStore

      if (storeToSearch.isEmpty) {
        None
      }
      else {
        val n = storeToSearch.dequeue()
        if (ord.lt(n, comparator.get)) {
          Some(n)
        }
        else {
          val toPush = comparator
          comparator = Some(n)
          currentSide = if (currentSide.isLeft) Right() else Left()
          toPush
        }
      }
    }
  }

  val mergeSortGraph = FlowGraph.partial() { implicit b =>
    import FlowGraph.Implicits._

    val merge = b.add(new LeftRightMarkedMerge[Int])
    val out = merge.out.transform { () => new Sorter() }.outlet
    UniformFanInShape(out, merge.left, merge.right)
  }
}

class DynamicMergeSort extends WordSpec {
  import DynamicMergeSort._

  implicit val sys = ActorSystem("DynamicMergeSort")
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

    def mergeSortN(expected: List[Int], ss: List[Int]*) = {
      val probe = FlowGraph.closed(TestSink.probe[Int]) { implicit b => sink =>
        import FlowGraph.Implicits._

        def joinGraph(s: IndexedSeq[Any], n: IndexedSeq[Any]): Unit = {
          if (s.size >= 2) {
            val merge = b.add(mergeSortGraph)
            s(0) match {
              case s: List[Int] => Source(s) ~> merge.in(0)
              case m: UniformFanInShape[Int, Int] => m ~> merge.in(0)
            }
            s(1) match {
              case s: List[Int] => Source(s) ~> merge.in(1)
              case m: UniformFanInShape[Int, Int] => m ~> merge.in(1)
            }
            joinGraph(s.tail.tail, n :+ merge)
          } else if (s.size == 1) {
            // The last output is here, connect it to the graph output
            if (n.isEmpty) s(0) match {
              case m: UniformFanInShape[Int, Int] => m ~> sink.inlet
            }
            //Not the last output, prepend the source
            else joinGraph(s(0) +: n, IndexedSeq.empty)
          } else joinGraph(n, IndexedSeq.empty)
        }

        joinGraph(ss.toIndexedSeq, IndexedSeq[Any]())
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

      "first source starts with a larger element than second" in {
        mergeSort(List(3, 4, 5), List(2, 8, 9), List(2, 3, 4, 5, 8, 9))
      }

      "3 sources" in {
        mergeSortN(List(2, 3, 4, 5, 8, 9), List(9), List(2, 8), List(3, 4, 5))
      }
    }

  }

}
