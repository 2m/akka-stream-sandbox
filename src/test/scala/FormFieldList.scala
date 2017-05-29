import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.RouteTest
import akka.http.scaladsl.testkit.TestFrameworkInterface.Scalatest
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model._
import akka.stream.Materializer

object FormFieldList {
  object Directives {
    import akka.http.scaladsl.unmarshalling.{ FromStrictFormFieldUnmarshaller ⇒ FSFFU, _ }
    import akka.http.scaladsl.unmarshalling.Unmarshaller._
    import akka.http.scaladsl.common._
    import akka.http.scaladsl.server.directives.FormFieldDirectives._
    import akka.http.scaladsl.server.directives.FormFieldDirectives.FieldDef._
    import akka.http.scaladsl.server._
    import scala.concurrent.Future
    import scala.util.{ Failure, Success }
    import akka.http.scaladsl.util.FastFuture._


    type SFU = FromEntityUnmarshaller[StrictForm]
    type FSFFOU[T] = Unmarshaller[StrictForm.Field, T]

    def formFieldList(pm: FieldMagnet): pm.Out = pm()

    implicit def forNR[T](implicit sfu: SFU, fu: FSFFU[T]) =
      extractField[NameReceptacle[T], Seq[T]] { nr ⇒ filter(nr.name, fu) }

    private def filter[T](fieldName: String, fu: FSFFOU[T])(implicit sfu: SFU): Directive1[Seq[T]] = {
      extract(fieldOfForm(fieldName, fu)).flatMap {
        onComplete(_).flatMap {
          case Success(x)                                  ⇒ provide(x)
          case Failure(Unmarshaller.NoContentException)    ⇒ reject(MissingFormFieldRejection(fieldName))
          case Failure(x: UnsupportedContentTypeException) ⇒ reject(UnsupportedRequestContentTypeRejection(x.supported))
          case Failure(x)                                  ⇒ reject(MalformedFormFieldRejection(fieldName, x.getMessage, Option(x.getCause)))
        }
      }
    }

    private def fieldOfForm[T](fieldName: String, fu: Unmarshaller[StrictForm.Field, T])(implicit sfu: SFU): RequestContext ⇒ Future[Seq[T]] = { ctx ⇒
      import ctx.executionContext
      implicit val mat: Materializer = ???
      sfu(ctx.request.entity).fast.flatMap { form ⇒
        val parts = form.fields.collect {
          case (name, part) if name == fieldName => fu(part)
        }
        Future.sequence(parts)
      }
    }

    private def extractField[A, B](f: A ⇒ Directive1[B]) = fieldDef(f)
  }
}

class FormFieldList extends WordSpec with RouteTest with Scalatest with Matchers {

  "list of form field values should be parsed" in {

    import FormFieldList.Directives._

    val route = path("add") {
      post {
        formFieldList('age.as[Int]) { age =>
          complete(age.toString)
        }
      }
    }

    val multipartForm = Multipart.FormData(
      Multipart.FormData.BodyPart.Strict("age", HttpEntity("42")),
      Multipart.FormData.BodyPart.Strict("age", HttpEntity("43"))
    )
    Post("/add", multipartForm) ~> route ~> check {
      response.status shouldBe OK
      unmarshalValue[String](response.entity) shouldBe "Vector(42, 43)"
    }
  }
}
