import spray.json._

case class Test(resourceState: JsValue)
case class Test2(resourceState: JsValue)

object Test extends DefaultJsonProtocol {

  type Resource[T] = {
    def toJson(implicit writer: JsonWriter[T]): JsValue
  }

  implicit object Test2Format extends RootJsonFormat[Test2] {
    def write(c: Test2) = JsObject("test2" -> c.resourceState)
    def read(value: JsValue) = Test2(JsTrue)
  }

  implicit object TestFormat extends RootJsonFormat[Test] {
    def write(c: Test) = JsObject(
      "resourceState" -> c.resourceState
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("resourceState") match {
        case Seq(resourceState) => {
          new Test(resourceState)
        }

        case _ => throw new DeserializationException("Test deserialization issue.")
      }
    }
  }
}

object StructuralJsonFormat extends App {

  import Test._

  val test: Resource[Test] = Test(JsTrue)
  printRes(test)

  val test2: Resource[Test2] = Test2(JsFalse)
  printRes(test2)

  def printRes[T](r: Resource[T]) = ???//println(r.toJson)

}
