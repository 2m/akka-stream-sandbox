import org.scalatest._
import akka.actor._
import akka.stream.scaladsl._
import org.scalatest.concurrent.ScalaFutures
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import com.typesafe.config.ConfigFactory

class AkkaStreamSourceRemoting extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  class SourceProvider extends Actor {
    def receive = {
      case _ => sender() ! akka.stream.scaladsl.Source(List(1,2,3))
    }
  }
  
  val config = ConfigFactory.parseString("""
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
        
        serialization-bindings {
          "akka.stream.scaladsl.Source" = java
        }
      }
      remote {
        enabled-transports = ["akka.remote.netty.tcp"]
        netty.tcp {
          hostname = "127.0.0.1"
        }
     }
    }""")
  
  val sys1 = ActorSystem("AkkaStreamSourceRemoting1", ConfigFactory.parseString("akka.remote.netty.tcp.port=2552").withFallback(config))
  val sys2 = ActorSystem("AkkaStreamSourceRemoting2", ConfigFactory.parseString("akka.remote.netty.tcp.port=2553").withFallback(config))
  import sys1.dispatcher
  implicit val timeout = Timeout(1 second)
  
  "a Source" should {
    
    val provider = sys1.actorOf(Props(new SourceProvider), "source")
    
    "be sent to other actor" in {
      whenReady(provider ? "GET ME TEH SOURCE") { result =>
        result shouldBe Source(List(1,2,3))
      }
    }
    
    "be sent to other remote actor" in {
      val remote = sys2.actorSelection("akka.tcp://AkkaStreamSourceRemoting1@127.0.0.1:2552/user/source").resolveOne()
      
      whenReady(remote.flatMap(_ ? "GET ME TEH SOURCE")) { result =>
        result shouldBe Source(List(1,2,3))
      }
    }
  }
  
  override protected def afterAll() = {
    super.afterAll()
    sys1.shutdown()
    sys1.awaitTermination()
    sys2.shutdown()
    sys2.awaitTermination()
  }
  
}