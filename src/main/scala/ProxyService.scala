import scala.concurrent.duration._
import scala.concurrent.Future
import akka.io.IO
import akka.http.Http
import akka.stream.scaladsl._
import akka.stream.FlattenStrategy
import akka.stream.{ ActorFlowMaterializerSettings, ActorFlowMaterializer }
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.util.{ Failure, Success, Try }
import akka.http.model.HttpMethods._
import akka.http.model.StatusCodes._
import akka.http.model._
import com.typesafe.config.{ ConfigFactory, Config }
import akka.stream.scaladsl.OperationAttributes._

/*object ProxyService extends App with Proxy {

  val configFile = """
    |akka {
    |  event-handlers = ["akka.event.Logging$DefaultLogger"]
    |  loglevel = "DEBUG"
    |  actor {
    |    provider = "akka.actor.LocalActorRefProvider"
    |  }
    |}
    |proxy {
    |   service {
    |     name = "ProxySystem"
    |     http.interface = 127.0.0.1
    |     http.port = 7080
    |   }
    |}""".stripMargin

  val config = ConfigFactory.parseString( configFile )

  val interface = config.getString( "proxy.service.http.interface" )
  val systemName = config.getString( "proxy.service.name" )
  val port = config.getInt( "proxy.service.http.port" ) // Resolve HTTP-port

  val systemRef = ActorSystem( systemName, config ) // Start actor system

  this.startService // Start application
}

trait Proxy {
  def interface: String
  def port: Int
  def systemRef: ActorSystem

  def startService {
    implicit val materializer = ActorFlowMaterializer()( systemRef )
    implicit val askTimeout: Timeout = 500.millis

    val serverBinding = Http( systemRef ).
      bind(
        interface = interface,
        port = port )

    val badRequestFlow: Flow[HttpRequest, HttpResponse] =
      Flow[HttpRequest].map( _ => HttpResponse( BadRequest, entity = "Bad Request" ) )

    val in = UndefinedSource[HttpRequest]
    val out = UndefinedSink[HttpResponse]
    val proxyTo = Flow[HttpRequest].map { _ =>
      println("yea")
      HttpRequest( GET, uri = "/?q=akka+streams" )
    }

    val con: Flow[HttpRequest, HttpResponse] =
      Http(systemRef).outgoingConnection( "www.google.com" ).flow

    val proxyBranch: Flow[HttpRequest, Branch[HttpRequest]] = Flow[HttpRequest].map {
      case req @ HttpRequest( GET, Uri.Path( "/proxy" ), _, _, _ ) => Branch( true, req )
      case other => Branch( false, other )
    }

    println("outgoing")
    Http(systemRef).outgoingConnection("www.google.com").flow
    println("outgoing2")

    val con2 = proxyTo.section(inputBuffer(4, 4))(_.via(Http(systemRef).outgoingConnection("www.google.com").flow))

    val partialBranch = PartialFlowGraph {
      implicit b =>
        import FlowGraphImplicits._
        val route = new BranchRoute[HttpRequest]
        val merge = Merge[HttpResponse]
        in ~> proxyBranch ~> route.in
        route.success ~> con2 ~> merge
        route.failure ~> badRequestFlow ~> merge
        merge ~> out
    }

    serverBinding.startHandlingWith( partialBranch.toFlow( in, out ) )
  }
}

case class Branch[T]( success: Boolean, message: T )

class BranchRoute[T] extends FlexiRoute[Branch[T]]( name("branchRoute") and inputBuffer(4, 4)) {
  import akka.stream.scaladsl.FlexiRoute._

  val success = createOutputPort[T]()
  val failure = createOutputPort[T]()

  override def createRouteLogic = new RouteLogic[Branch[T]] {
    override def outputHandles( outputCount: Int ) = Vector( success, failure )

    override def initialState = State[T]( DemandFromAll( success, failure ) ) {
      case ( ctx, _, Branch( successState, element ) ) =>
        if ( successState ) ctx.emit( success, element )
        else ctx.emit( failure, element )
        SameState
    }
  }
}*/
