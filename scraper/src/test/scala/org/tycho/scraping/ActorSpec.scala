package org.tycho.scraping
import org.tycho.scraping._
import org.scalatest._
import akka.actor._
//import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
//import akka.testkit.TestActorRef  //recommended to test with messages instead
//import akka.testkit.{ TestActorRef, TestActors, TestKit, ImplicitSender }
import akka.testkit._
//import scala.util.{Success, Failure}
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
//import org.scalatest.WordSpecLike
//import org.scalatest.Matchers
//import org.scalatest.BeforeAndAfterAll
import org.scalatest.prop._
import org.tycho.scraping.AkkaScraping._
import scala.reflect.ClassTag
import scala.reflect.api.TypeTags
import scala.reflect.runtime.universe._

class ActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {
//with UnitSpec with ShouldMatchers with GeneratorDrivenPropertyChecks

  def this() = this(ActorSystem("MySpec"))
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
  
  implicit class MyTestActorRef[T <: Actor](val actor: TestActorRef[T]) {
    
    def checkHandled(handled: List[Any], unhandled: List[Any] = List(Nil)) {
      actor.underlyingActor.getClass.getSimpleName() must {
        handled.foreach{ msg: Any => actor.shouldHandle(msg) }
        unhandled.foreach{ msg: Any => actor.dontHandle(msg) }
      }
    }
    
    def shouldHandle(msg: Any) {
      "handle " + msg.getClass.getSimpleName() in {
        try {
          actor receive msg
      //      expectMsg("A")
        } catch {
          case e: TypeNotHandledException => {
            fail(msg.getClass.getSimpleName() + s" unhandled!")  // $msg
          }
//          case e: ClassCastException => {
//            fail(e)
//          }
          case e: Exception => {
            fail(e)
          }
        }
      }
    }
    
    def dontHandle(msg: Any) {
      "not handle " + msg.getClass.getSimpleName() in {
  //      intercept[TypeNotHandledException] {
  //        actor receive msg
  //      }
        evaluating {
          actor receive msg
        } should produce [TypeNotHandledException]
      }
    }
  }
  
//  implicit val system = ActorSystem()
  implicit val timeout = Timeout(5 seconds)
  
}
