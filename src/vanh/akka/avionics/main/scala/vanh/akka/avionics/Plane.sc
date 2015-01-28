//Package vanh.akka.avionics

import akka.actor.{Props, Actor, ActorLogging}
import vanh.akka.avionics.{LeadFlightAttendant, PilotProvider}

object Plane {
  // give control surface any actor that want it
  case object GiveMeControl
}
class Plane extends Actor with ActorLogging{

  import Altimeter._
  import Plane._

  //can't directly new up the altimeter
  //must use context.actorOf method to create a new
  // actorRef and tie it to this actor as a child
  val altimeter = context.actorOf(Props[Altimeter],"Altimeter")
  val controls = context.actorOf(Props(new ControlSurface(altimeter)),"ControlSurface")

  def receive = {
    // some one ask for control
    case GiveMeControl => log info "Plane giving control."
      sender ! controls // send them the control ref
  }
}