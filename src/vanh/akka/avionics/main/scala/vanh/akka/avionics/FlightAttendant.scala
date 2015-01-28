package vanh.akka.avionics

/**
 * Created by vanh on 1/20/15.
 */

import akka.actor.{Cancellable, Actor,ActorRef}
import scala.concurrent.duration._

trait AttendentResponsiness {
  val maxResponseTimeMS: Int
  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis
}
trait FlightAttendantProvider{
  def newFlightAttendant:Actor = FlightAttendant()
}

object FlightAttendant {

  case class GetDrink(drinkname: String)
  case class Drink(drinkname: String)
  case class Assist(passenger:ActorRef)
  case object Busy_?
  case object Yes
  case object No

  def apply() = new FlightAttendant with AttendentResponsiness {
    val maxResponseTimeMS = 300000 // 5min
  }
}

class FlightAttendant extends Actor {
  this: AttendentResponsiness =>
  import FlightAttendant._

  // bring execution context into implicit scope for scheduler
  implicit val ec = context.dispatcher
 // def receive = {

      //internal msg signal that drink is going to deliver
      case class DeliverDrink(drink: Drink)
      // store timer an instance of 'cancellable'
      var pendingDelivery:Option[Cancellable] = None
      //schedule a delivery
      def scheduleDelivery(drinkname:String):Cancellable = {
        context.system.scheduler.scheduleOnce(responseDuration,self,DeliverDrink(Drink(drinkname)))
      }
      // state handling emergency
      def assistInjuredPassenger:Receive = {
        case Assist(passenger) =>
          //stop everything and assist now!
        pendingDelivery foreach {_.cancel()}
        pendingDelivery = None
        passenger ! Drink ("Magic drug")
      }
       //state handling normal task and not busy servicing request
      def handleDrinkRequests:Receive = {
        case GetDrink(drinkname) =>
          //schedule a response and the sender ref is frozen at schedule time
          //context.system.scheduler.scheduleOnce(responseDuration,sender(),Drink(drinkname))
          pendingDelivery = Some(scheduleDelivery(drinkname))
          //try to assist injure passenger
          context.become(assistInjuredPassenger orElse handleSpecificPerson(sender()))
        case Busy_? => sender ! No
      }
      //switch to this state from servicing the drink
      def handleSpecificPerson(person:ActorRef):Receive = {
        //this is a request from the same one cancel previous request and work new request
        case GetDrink(drinkname) if sender() == person =>
          pendingDelivery foreach{_.cancel()}
          pendingDelivery = Some(scheduleDelivery(drinkname))
          //the state where deliverdrink msg come in
        case DeliverDrink(drink) =>
          person ! drink
          pendingDelivery = None
          //become something else
          context.become(assistInjuredPassenger orElse handleDrinkRequests)
        //getting another request too busy pass on to the lead
        case m: GetDrink => context.parent forward m
        case Busy_? => sender ! Yes
      }
      //set up the initial handler
      def receive = assistInjuredPassenger orElse handleDrinkRequests

}

