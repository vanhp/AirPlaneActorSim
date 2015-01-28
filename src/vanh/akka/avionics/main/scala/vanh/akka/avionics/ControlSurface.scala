package vanh.akka.avionics

/**
 * Created by vanh on 1/17/15.
 *
 */

//import _root_.Altimeter.RateChange
import akka.actor.{Actor, ActorRef}

//control surface carry msg to control the plane
object ControlSurface {
  // the control joystick
  //amount of forward/backward movement between -1,1 or it's be truncate
  case class StickBack(amount:Float)
  case class StickForward(amount:Float)

  case class StickLeft(amount:Float)
  case class StickRight(amount:Float)
  //who is in charge
  case class HasControl(somePilot:ActorRef)
}

class ControlSurface(plane: ActorRef,heading:ActorRef,altimeter: ActorRef) extends Actor{
  import ControlSurface._
  import Altimeter._
  import HeadingIndicator._

  //start out with noone in control but deadletter
  def receive = controlledBy(context.system.deadLetters)
  //the receive get reinstantiate with new controller in charge
  // this closure make sure that only one pilot can control the plane at a time
  def controlledBy(somePilot:ActorRef):Receive = {
        //the stick get pull backward notify altimeter we're climbing
        case StickBack(amount) if sender == somePilot => altimeter ! RateChange(amount)
        // stick is pull forward descending
        case StickForward(amount) if sender == somePilot => altimeter ! RateChange(-1 * amount)

          //heading control
        case StickLeft(amount) if sender == somePilot => heading ! BankChange(-1 * amount)
        case StickRight(amount) if sender == somePilot => heading ! RateChange( amount)

          //the plane will tell the entity that controlling it
        case HasControl(entity) if sender == plane =>
          //morph into the entity that control the plane
          context.become(controlledBy(entity))

  }
}
