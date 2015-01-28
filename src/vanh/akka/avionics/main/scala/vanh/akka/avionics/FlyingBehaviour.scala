package vanh.akka.avionics

import akka.actor.{Props, Actor, FSM, ActorRef}

/**
 * Created by vanh on 1/26/15.
 */

trait FlyingProvider {
  def newFlyingBehaviour(plane: ActorRef,heading:ActorRef,altimeter: ActorRef):Props =
    Props(new FlyingBehaviour(plane,heading,altimeter))
}

object FlyingBehaviour {
  import ControlSurface._

  //the FSM states. These state can send
  // and receive msg
  sealed trait State
  case object Idle extends State
  case object Flying extends State
  case object PreparingToFly extends State

  //msg to change calculation function
  case class NewElevatorCalculator(f:Calculator)
  case class NewBankCalculator(f:Calculator)

  //helper classes to hold some data
  case class CourseTarget(altitude:Double,heading:Float,byMillis:Long)
  case class CourseStatus(altitude:Double,heading:Float,headingSinceMS:Long,altitudeSinceMS:Long)

  //change the way to calc
  type Calculator = (CourseTarget,CourseStatus) => Any

  //flyingbehaviour data
  sealed trait Data
  case object Uninitialized extends Data
  case class FlightData(controls:ActorRef,
                        elevCalc:Calculator,
                        bankCalc:Calculator,
                        target: CourseTarget,
                        status:CourseStatus) extends Data
  case class Fly(target: CourseTarget)

  def currentMS = System.currentTimeMillis()
  def calcElevator(target: CourseTarget,status: CourseStatus):Any = {
    val alti = (target.altitude - status.altitude).toFloat
    val dur = target.byMillis - status.altitudeSinceMS
    if(alti < 0) StickBack(alti / dur)
  }
  //banking change
  def calcAilerons(target: CourseTarget,status: CourseStatus):Any = {
    import scala.math._
    val diff = target.heading - status.heading
    val dur = target.byMillis - status.headingSinceMS
    val amount = if(abs(diff)< 180) diff else signum(diff) * (abs(diff) - 360f)
    if (amount > 0) StickRight(amount / dur) else StickLeft((amount / dur) * -1)

  }
}

class FlyingBehaviour(plane: ActorRef,heading:ActorRef,altimeter: ActorRef) extends Actor
  with FSM[FlyingBehaviour.State,FlyingBehaviour.Data]{
  import FSM._
  import FlyingBehaviour._
  import Pilots._
  import Plane._
  import Altimeter._
  import HeadingIndicator._
  import EventSource._

  import scala.concurrent.duration._

  case object Adjust

  //setup the state init value
  startWith(Idle,Uninitialized)

  // adjust heading,alitude according to the new calc
  // and return flightdata
  def adjust(flightData: FlightData):FlightData = {
    val FlightData(c,elevCalc,bankCalc,t,s) = flightData
    c ! elevCalc(t,s)
    c ! bankCalc(t,s)
    flightData
  }

  //move to next state preparetofly when get the event
  when(Idle){
    case Event(Fly(target),_) =>
        goto(PreparingToFly) using FlightData(
              context.system.deadLetters,
              calcElevator,
              calcAilerons,
              target,
              CourseStatus(-1,-1,0,0))
  }
  // need some current info before prepare to fly
  // send msg request them
  onTransition {
    // transistion to ready to fly
    case Idle -> PreparingToFly =>
      plane ! GiveMeControl
      heading ! RegisterListener(self)
      altimeter ! RegisterListener(self)
  }
  def prepComplete(data:Data):Boolean = {
    data match {
      case FlightData(c,_,_,_,s) =>
        if(!c.isTerminated && s.heading != -1f && s.altitude != -1f)
          true
        else
          false
      case _ => false
    }
  }
  when(PreparingToFly,stateTimeout = 5.seconds)(transform {
    //must have all events info before flying
    // it's ok to get more than one eventhandler
    //get heading info
    case Event(HeadingUpdate(head),d:FlightData) =>
      stay using d.copy(status = d.status.copy(heading = head,headingSinceMS = currentMS))
    //get altitude info
    case Event(AltitudeUpdate(alt),d:FlightData) =>
      stay using d.copy(status = d.status.copy(altitude = alt,altitudeSinceMS = currentMS))
      //get stick control info
      //   case Event(Controls(ctrl),d:FlightData) =>
      //   stay using d.copy(controls = ctrl)
      //send the time out only if don't get any msg event
      // case Event(StateTimeout,_) => plane ! LostControl
      goto(Idle)
  }
    using //pass the output (state data and state name)from event handler block of code
    // into prepcomplete block of code
    {
      case s if prepComplete(s.stateData) => s.copy(stateName = Flying)
    })

 onTransition {
   //transition to flying
   case PreparingToFly -> Flying =>
     //get it time to adjust the plane controller
     setTimer("Adjustment", Adjust, 200.milliseconds, repeat = true)
 }
  when(Flying) {
    case Event(HeadingUpdate(head),d:FlightData) =>
      stay using d.copy(status = d.status.copy(heading = head,headingSinceMS = currentMS))
    //get altitude info
    case Event(AltitudeUpdate(alt),d:FlightData) =>
      stay using d.copy(status = d.status.copy(altitude = alt,altitudeSinceMS = currentMS))
    case Event(Adjust,flightdata:FlightData) => stay using adjust(flightdata)
      //let other switch out the calculation function
    case Event(NewBankCalculator(f),d:FlightData) => stay using d.copy(bankCalc = f)
    case Event(NewElevatorCalculator(f),d:FlightData) => stay using d.copy(elevCalc = f)
  }

  onTransition {
    //transition to not flying clean up
    case Flying -> _ =>
      cancelTimer("Adjustment")
  }

  onTransition{
    //transition to idle state clean up too
    case _ -> Idle =>
      heading ! UnregisterListener(self)
      altimeter ! UnregisterListener(self)
  }

  // take care of an unexpect event like release control
  //msg from the plane. We lost control to to idle state
  whenUnhandled{
    case Event(RelinquishControl, _) =>
      goto(Idle)
  }
  initialize() //house keeping requirement from FSM
}