import java.nio.file.Path
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source}
import spray.json.DefaultJsonProtocol

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object ClientMultipartRequest {

  object CentaurConfig {
    val cromwellUrl = ""
  }
  val apiPath = ""

  case class FileData(size: Int, path: Path, getFileName: String)
  case class FileParam(file: FileData)
  case class Data(wdl: FileParam, inputs: Option[FileParam], options: Option[FileParam])
  case class Workflow(data: Data)
  case class SubmittedWorkflow(uuid: UUID, url: String, wf: Workflow)

  case class CromwellStatus(id: String)

  def insertSecrets(s: Option[FileParam]) = s

  def sendReceiveFutureCompletion[A](f: Future[A]): Try[A] = ???

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val cromwellStatusFormat = jsonFormat1(CromwellStatus)
  }

  def submit(workflow: Workflow)(implicit sys: ActorSystem, mat: Materializer, ec: ExecutionContext): Try[SubmittedWorkflow] = {

    // edited: params is a map of parameter names and content of files associated to each. All these parameters are
    //of the type formData.
    val params: Map[String, FileParam] = Map(
      "wdlSource" -> Option(workflow.data.wdl),
      "workflowInputs" -> workflow.data.inputs,
      "workflowOptions" -> insertSecrets(workflow.data.options)
    ) collect { case (name, Some(value)) => (name, value) }

    //Step 1: Converted the params + associated files to Iterable[Multipart.FormData.BodyPart]
    val bodyParts = params map { case (name, value) =>
      val file = value.file
      Multipart.FormData.BodyPart(
        name, HttpEntity(MediaTypes.`application/json`, file.size, FileIO.fromPath(file.path, 100000)), Map(name -> value.file.getFileName)) }

    //Step 2: Wrap the iterable BodyPart objects into a Source objects -- Do I have to do this?
    val sources = Source(bodyParts)

    //Step 3: Wrap the sources into a Multipart.FormData object
    val multiPartFormData = Multipart.FormData(sources)

    //Step 4: Convert the multiPartFormData into a RequestEntity
    val myEntity = multiPartFormData.toEntity()

    //Step 5: Create a Seq of HTTP headers
    val myHeaders = immutable.Seq.empty[HttpHeader] //not implemented

    import JsonProtocol._
    import SprayJsonSupport._

    val submittedWorkflow = for {
      response <- Http().singleRequest(HttpRequest(HttpMethods.POST, CentaurConfig.cromwellUrl + apiPath, myHeaders, myEntity))
      entity <- Unmarshal(response).to[CromwellStatus]
    } yield SubmittedWorkflow(UUID.fromString(entity.id), CentaurConfig.cromwellUrl, workflow)
    sendReceiveFutureCompletion(submittedWorkflow)
  }

}
