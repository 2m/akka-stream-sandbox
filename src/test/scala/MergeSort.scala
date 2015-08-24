import akka.actor.ActorSystem
import akka.stream._
import akka.stream.FanInShape._
import akka.stream.scaladsl._
import akka.stream.stage._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

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
    private var store = Option.empty[A]

    override def onPush(elem: A, ctx: Context[A]): SyncDirective = store match {
      case Some(n) if implicitly[Ordering[A]].lt(n, elem) =>
        store = Some(elem)
        ctx.push(n)
      case Some(n) =>
        ctx.push(elem)
      case None =>
        store = Some(elem)
        ctx.pull()
    }

    override def onPull(ctx: Context[A]): SyncDirective = store match {
      case Some(n) if ctx.isFinishing =>
        ctx.pushAndFinish(n)
      case _ =>
        ctx.pull()
    }

    override def onUpstreamFinish(ctx: Context[A]): TerminationDirective = store match {
      case Some(n) => ctx.absorbTermination()
      case None => ctx.finish()
    }

  }

  val mergeSortGraph = FlowGraph.partial() { implicit b =>
    import FlowGraph.Implicits._

    val merge = b.add(new LeftRightMerge[Int])
    val out = merge.out.transform { () => new Sorter() }.outlet
    UniformFanInShape(out, merge.left, merge.right)
  }
}

class MergeSort extends WordSpec with Matchers with ScalaFutures {
  import MergeSort._

  implicit val sys = ActorSystem("MergeSort")
  implicit val mat = ActorMaterializer()

  "merge sort" should {

    "sort in order" in {
      val future = FlowGraph.closed(Sink.head[Seq[Int]]) { implicit b => sink =>
        import FlowGraph.Implicits._
        val ms = b.add(mergeSortGraph)
        Source(List(1, 2, 4, 10)) ~> ms.in(0)
        Source(List(3, 5, 8, 9)) ~> ms.in(1)
        ms.out.grouped(8) ~> sink
      }.run()

      whenReady(future) { _ shouldBe Seq(1, 2, 3, 4, 5, 8, 9, 10) }
    }
  }

}
