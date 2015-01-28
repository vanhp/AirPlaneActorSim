package vanh.akka.avionics

/**
 * Created by vanh on 1/20/15.
 *
 */

import akka.actor.{Actor,ActorRef,Props}

//how the lead create its helper attendant
trait AttendantCreationPolicy {
  val numberOfAttendants: Int = 8 //may be configurable
  def createAttendant: Actor = FlightAttendant()
}

trait  LeadFlightAttendantProvider {
  def newFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant {
  case object GetFlightAttendant
  case class Attendant(a:ActorRef)
  def apply() = new LeadFlightAttendant  with AttendantCreationPolicy
}

class LeadFlightAttendant extends Actor {
  this: AttendantCreationPolicy =>
  import LeadFlightAttendant._

  // start up lead attendant
  override def preStart(): Unit = {
    import scala.collection.JavaConverters._
    val attendantNames =
      context.system.settings.config.getStringList("vanh.akka.avionics.flightcrew.attendantNames").asScala
    //create its children actors helper
    attendantNames take numberOfAttendants foreach { i =>
      context.actorOf(Props(createAttendant),i)}
  }
  //pick a random actor
  def randomAttendant(): ActorRef = {
    context.children.take(scala.util.Random.nextInt(numberOfAttendants) + 1).last
  }
  def receive = {
    case GetFlightAttendant =>
      sender() ! Attendant(randomAttendant())
    case m =>
      randomAttendant() forward m
  }
}



//testing code
object FlightAttendantPathChecker {
  def main(args: Array[String]) {
    val system = akka.actor.ActorSystem("PlaneSimulation")
    val lead = system.actorOf(Props(
      new LeadFlightAttendant with AttendantCreationPolicy),
      "LeadFlightAttendant")
    Thread.sleep(2000)
    system.shutdown()
  }
}