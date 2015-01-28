/**
 * Created by vanh on 1/20/15.
 */

//package vanh.akka.avionics
//
//import akka.actor.{Props, ActorSystem}
//import akka.testkit.{TestKit, TestActorRef, ImplicitSender}
//import org.scalatest.WordSpec
//import org.scalatest.matchers.MustMatchers
//
//object TestFlightAttendant {
//  def apply() = new FlightAttendant
//    with AttendantResponsiveness {
//    val maxResponseTimeMS = 1
//  }
//}

//for performance set duration to mil instead of
//default 100 millis
//import com.typesafe.config.ConfigFactory
//class FlightAttendantSpec extends
//TestKit(ActorSystem("FlightAttendantSpec",
//  ConfigFactory.parseString(
//    "akka.scheduler.tick-duration = 1ms"))



//class FlightAttendantSpec extends
//TestKit(ActorSystem("FlightAttendantSpec"))
//with ImplicitSender
//with WordSpec
//with MustMatchers {
//  import FlightAttendant._
//  "FlightAttendant" should {
//    "get a drink when asked" in {
//      val a = TestActorRef(Props(TestFlightAttendant()))
//      a ! GetDrink("Soda")
//      expectMsg(Drink("Soda"))
//    }
//  }
//}

