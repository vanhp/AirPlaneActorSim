//Package vanh.akka.avionics

import akka.actor.{Actor, ActorRef}

//control surface carry msg to control the plane
object ControlSurface {
  //amount of forward/backward movement between -1,1 or it's be truncate
  case class StickBack(amount:Float)
  case class StickForward(amount:Float)
}

class ControlSurface(altimeter: ActorRef) extends Actor{
  import ControlSurface._
  import Altimeter._

  def receive = {
    //the stick get pull backward notify altimeter we're climbing
    case StickBack(amount) => altimeter ! RateChange(amount)
    // stick is pull forward descending
    case StickForward(amount) => altimeter ! RateChange(-1 * amount)


  }
}