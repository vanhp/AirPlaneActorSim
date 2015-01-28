/**
* Created by vanh on 1/20/15.
*/

package vanh.akka.avionics
// for testing

import akka.actor.{Props,ActorSystem, Actor}
import akka.testkit.{TestKit,TestActorRef,ImplicitSender}
import org.scalatest.{WordSpec,BeforeAndAfterAll}
import org.scalatest.matchers.MustMatchers

//class EventSource extends EventSource {this: =>
//  import EventSource._
//  override def afterAll() = {
//    system.shutdown()
//  }
//  "EvenSource" should {
//    "allow us to register a listener" in {
//      val real = TestActorRef[TestEventSource].underlyingActor
//      real.receive(RegisterListener(testActor))
//      real.Listeners must contain (testActor)
//    }
//    "allow us to unregister a listener" in {
//      val real = TestActorRef[TestEventSource].underlyingActor
//      real.receive(RegisterListener(testActor))
//      real.receive(UngisterListener(testActor))
//      real.Listeners.size must be (0)
//    }
//    "send the event to our test actor" in {
//      val testA = TestActorRef[TestEventSource]
//      testA ! RegisterListener(testActor)
//      testA.underlyingActor.sendEvent("Fibonacci")
//      expectMsg("Fibonacci")
//    }
//  }
//}
//
//
//
//
//// for testing evensource trait only
class TestEventSource extends Actor with ProductionEventSource {
  def receive = eventSourceReceive
}

// trait wordSpec extends TestKit{this:WordSpec =>
// implicit def self = Actor}


//class EventSourceSpec extends TestKit(ActorSystem("EventSourceSpec"))
//            with WordSpec
//            with MustMatchers
//            with BeforeAndAfterAll {
//  import EventSource._
//  override def afterAll() = {system.shutdown()}
//
//  "EvenSource" should {
//    "allow us to register a listener" in {
//      val real = TestActorRef[TestEventSource].underlyingActor
//      real.receive(RegisterListener(testActor))
//      real.Listeners must contain (testActor)
//    }
//    "allow us to unregister a listener" in {
//      val real = TestActorRef[TestEventSource].underlyingActor
//      real.receive(RegisterListener(testActor))
//      real.receive(UngisterListener(testActor))
//      real.Listeners.size must be (0)
//    }
//    "send the event to our test actor" in {
//      val testA = TestActorRef[TestEventSource]
//      testA ! RegisterListener(testActor)
//      testA.underlyingActor.sendEvent("Fibonacci")
//      expectMsg("Fibonacci")
//    }
//  }
//}