import scala.io.StdIn
import scala.util._

object Util {
  def repl(main: String => Option[String]) = {
    @annotation.tailrec
    def action(): Unit = Try(main(StdIn.readLine)) match {
      case Success(Some(output)) => println(output); action()
      case Success(None) => // stop repl
      case Failure(ex) => println(s"Unrecognized input: $ex. Type 'q' to quit."); action()
    }
    action()
  }

  object Int {
    def unapply(s : String) : Option[Int] = try {
      Some(s.toInt)
    } catch {
      case _ : java.lang.NumberFormatException => None
    }
  }

  implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }
}
