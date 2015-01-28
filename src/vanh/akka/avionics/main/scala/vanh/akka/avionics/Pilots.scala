package vanh.akka.avionics

/**
 * Created by vanh on 1/21/15.
 *
 */
import akka.actor.{FSM, Terminated, Actor, ActorRef}

trait PilotProvider {
//  def pilot:Actor = new Pilot
//  def copilot:Actor = new CoPilot
//  def autopilot:Actor = new AutoPilot
 // def newPilot(plane:ActorRef,controls:ActorRef,altimeter: ActorRef): Actor =

  def newPilot(plane:ActorRef,autopilot:ActorRef,heading:ActorRef,altimeter: ActorRef): Actor =
    new Pilot(plane,autopilot,heading,altimeter) with DrinkingProvider with FlyingProvider
   // new Pilot(plane,controls,altimeter) with DrinkingProvider with FlyingProvider
  def newCoPilot(plane:ActorRef,altimeter: ActorRef): Actor =
    new CoPilot(plane,altimeter)
}

object Pilots {
  import FlyingBehaviour._
  import ControlSurface._

  case object ReadyToGo
  case object RelinquishControl

  //calc change depend on feeling tipsy
  val tipsyCalcElevator:Calculator = {
    (target,status) =>
      val msg = calcElevator(target,status)
      msg match {
        case StickForward(amt) => StickForward(amt * 1.03f)
        case StickBack(amt) => StickBack(amt * 1.03f)
        case m => m
      }
  }
  //calc change depend on feeling tipsy
  val tipsyCalcAileron:Calculator = {
    (target,status) =>
      val msg = calcAilerons(target,status)
      msg match {
        case StickLeft(amt) => StickLeft(amt * 1.03f)
        case StickRight(amt) => StickRight(amt * 1.03f)
        case m => m
      }
  }
  //calc change depend on feeling zaphod
  val ZaphodCalcElevator:Calculator = {
    (target,status) =>
      val msg = calcElevator(target,status)
      msg match {
        case StickForward(amt) => StickBack(1f)
        case StickBack(amt) => StickForward(1f)
        case m => m
      }
  }
  val ZaphodCalcAileron:Calculator = {
    (target,status) =>
      val msg = calcAilerons(target,status)
      msg match {
        case StickLeft(amt) => StickRight(1f)
        case StickRight(amt) => StickLeft(1f)
        case m => m
      }
  }
}

class Pilot(plane: ActorRef,
            autopilot:ActorRef,
            heading:ActorRef,
            alitmeter:ActorRef)extends Actor {
  this: DrinkingProvider with FlyingProvider =>
  import Pilots._
 // import Pilot._
  import Plane._
  import Altimeter._
  import ControlSurface._
  import DrinkingBehaviour._
  import FlyingBehaviour._
  import FSM._


  var control:ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
 // var autopilot = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("vanh.akka.avionics.flightcrew.copilotName")

  def setCourse(flyer:ActorRef): Unit ={
    flyer ! Fly(CourseTarget(20000,250,System.currentTimeMillis + 30000))
  }
  override def preStart(): Unit ={
    //create children take advantage of the fact that we’re about to become and pass the
    //values in from the bootstrap behaviour to the sober behaviour as closed-over
    //values. This is just another way to reference the data in your actors, and is
     // facilitated by the state changes we’re implementing.
    context.actorOf(newDrinkingBehaviour(self),"DrinkingBehaviour")
    context.actorOf(newFlyingBehaviour(plane,heading,alitmeter),"FlyingBehaviour")
  }

 // a custom receive method. only in this state once
  def bootstrap:Receive = {
    case ReadyToGo =>
     val copilot = context.actorFor("../" + copilotName)
     val flyer = context.actorFor("FlyingBehaviour")
     flyer ! SubscribeTransitionCallBack(self)
     setCourse(flyer)
     context.become(sober(copilot,flyer))

//      //pilot is a child of plane
//      context.parent ! Plane.GiveMeControl
//      copilot = context.actorFor("../" + copilotName)
//      autopilot = context.actorFor("../AutoPilot")
//    //  case controls(controlSurfaces) =>
//    //    control = controlSurfaces
  }
  def sober(copilot: ActorRef,flyer:ActorRef): Receive = {
    case FeelingSober => //sober
    case FeelingTipsy => becomeTipsy(copilot,flyer)
    case FeelingLikeZaphod => becomeZaphod(copilot,flyer)
  }
  def tipsy(copilot: ActorRef,flyer:ActorRef):Receive = {
    case FeelingSober => becomeSober(copilot,flyer)
    case FeelingTipsy => //same
    case FeelingLikeZaphod => becomeZaphod(copilot,flyer)
  }
  def zaphod(copilot:ActorRef,flyer:ActorRef):Receive = {
    case FeelingSober => becomeSober(copilot,flyer)
    case FeelingTipsy => becomeTipsy(copilot,flyer)
    case FeelingLikeZaphod => // already here
  }
  def becomeTipsy(copilot: ActorRef,flyer:ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(sober(copilot,flyer))
  }
  def becomeSober(copilot: ActorRef,flyer:ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(tipsy(copilot,flyer))
  }
  def becomeZaphod(copilot: ActorRef,flyer:ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(zaphod(copilot,flyer))
  }
  //at this state pilot do nothing!
  def idle:Receive = {
    case _ =>
  }

  override def unhandled(msg:Any):Unit = {
    msg match {
      case Transition(_,_,Flying) => setCourse(sender)
      case Transition(_,_,Idle) => context.become(idle)
        //ignore msg from FSM so it won't go into log
      case Transition(_,_,_) =>
      case CurrentState(_,_) =>
      case m => super.unhandled(m) //send them to parent
    }
  }
  //initially starting state call our custom receive
  def receive = bootstrap
}

class CoPilot(plane: ActorRef,altimeter: ActorRef) extends Actor {
  import Pilots._

  var control:ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("vanh.akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
      context.watch(pilot) //dead watch pilot
      autopilot = context.actorFor("../AutoPilot")
    case Terminated(_) => //pilot dead
      plane ! Plane.GiveMeControl
  }
}

class AutoPilot(plane:ActorRef) extends Actor {
  import Pilots._

  var control:ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters

  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" )
      copilot = context.actorFor("../AutoPilot")
      context.watch(copilot)
    case Terminated(_) =>
      plane ! Plane.GiveMeControl
  }
}