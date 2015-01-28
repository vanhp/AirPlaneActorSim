//package vanh.akka.avionics
//
//import akka.actor.{Props, ActorRef,ActorSystem,Actor}
//import org.scalatest.{ParallelTestExecution, MustMatchers, WordSpec}
//import akka.testkit.{TestKit,TestActorRef,ImplicitSender}
//import org.scalatest.{WordSpec,BeforeAndAfter,ParallelTestExecution}
//import java.util.concurrent.atomic.AtomicInteger
////import org.scalatest.matchers.MustMatchers
//import scala.sys.Prop
//
///**
//* Created by vanh on 1/20/15.
//*/
//class MyActorSpec extends WordSpec with MustMatchers with ParallelTestExecution {
//  def makeActor():ActorRef = system.actorOf(Props[MyActor],"MyActor")
//  "My Actor" should {
//    "throw when made with the wrong name" in new ActorSys {
//      evaluating {
//        // use a generated name
//        val a = system.actorOf(Props[MyActor])
//      } must produce [Exception]
//    }
//    "construct without exception" in new ActorSys {
//      val a = makeActor()
//      // The throw will cause the test to fail
//    }
//    "respond with a Pong to a Ping" in new ActorSys {
//      val a = makeActor()
//      a ! Ping
//      expectMsg(Pong)
//    }
//  }
//}
