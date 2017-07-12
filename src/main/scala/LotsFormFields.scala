import akka.http.scaladsl.server.HttpApp
import scala.concurrent.duration._

object WebServer extends HttpApp {

  import CustomDirectives._

  def routes =
    toStrictEntity(3.seconds) {
      path("hello") {
        formField('f25.as[Int], 'f2, 'f3, 'f4, 'f5, 'f6, 'f7, 'f8, 'f9, 'f10, 'f11, 'f12, 'f13, 'f14, 'f15, 'f16, 'f17, 'f18, 'f19, 'f20, 'f21, 'f22) {
          (f25, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21, f22) =>
            formFields('f1, 'f24, 'f25) { (f1, f24, f25) =>
              complete(f1 + f25)
            }
        }
      } ~
      path("ohi") {
        //unlimitedFormFields('f1.as[Int]) { i =>
          complete("success")
        //}
      }
    }
}

object LotsFormFields extends App {
  WebServer.startServer("localhost", 8080)
}

object CustomDirectives {
  import akka.http.scaladsl.common.NameReceptacle
  import akka.http.scaladsl.unmarshalling.{ FromStringUnmarshaller => FSU, Unmarshaller }
  import akka.http.scaladsl.server._
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.util._
  import akka.stream._
  import scala.concurrent.ExecutionContext
  import scala.util._

  def unlimitedFormFields(m: FormFieldsMagnet): m.Out = m()

  sealed trait FormFieldsMagnet {
    type Out
    def apply(): Out
  }

  /*implicit def fromNameReceptacle[T](nr: NameReceptacle[T])(implicit fsu: FSU[T], materializer: Materializer, ec: ExecutionContext) =
    new FormFieldsMagnet {
      type Out = Directive1[List[T]]
      def apply() =
        formFieldMap.flatMap { multiMap =>
          val fieldList = multiMap.getOrElse(nr.name, List.empty).map { value =>
            onComplete(fsu(value)) flatMap {
              case Success(x) => provide(x)
              case Failure(x) => reject(MalformedQueryParamRejection(nr.name, x.getMessage, Option(x.getCause))).toDirective(Tuple.forTuple1[T])
            }
          }

          Directive.sequence(fieldList)
        }
    }*/

  object Directive {
    def sequence[T](directives: List[Directive1[T]]) =
      directives.foldLeft(provide(List.empty[T])) {
        (dl, da) => dl.flatMap { l => da.flatMap(a => provide(l :+ a)) }
      }
  }
}
