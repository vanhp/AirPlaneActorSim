package vanh.akka.avionics

/**
 * Created by vanh on 1/17/15.
 *
 */
import akka.actor.{Actor, ActorLogging,ActorSystem}
import scala.concurrent.duration._

trait AltimeterProvider {
  def newAltimeter:Actor = Altimeter()
}
// the altimeter of the plane
object Altimeter{
  // the msg that it response to

  //receive msg from controlsurface
  case class RateChange(amount: Float)
  // send out altitude update to listeners
  case class AltitudeUpdate(altitude:Double)
  //change the factory method
  def apply() = new Altimeter with ProductionEventSource
}


class Altimeter extends Actor with ActorLogging with ProductionEventSource {//this: EventSource => // with EventSource { //this: EventSource =>
  import Altimeter._

  // create a dispatcher to schedule it's own work on this thread
  implicit val ec = context.dispatcher

  // max altitude for the plane feet
  val ceiling = 43000
  //max climb rate feet/m
  val maxRateOfClimb = 5000
  //climb rate
  var rateOfClimb = 0f
  //current altitude
  var altitude = 0d
  //time passed use to change altitude
  var lastTick = System.currentTimeMillis()
  // periodic update the altitude by sending ourself a msg on every 100ms
  val ticker = context.system.scheduler.schedule(100.millis,100.millis,self,Tick)

  //self notify msg to update altitude
  case object Tick

  //reactive programming react to event from other source instead of
  //active prog which take command from other then do the work
  // this encourage autonomous design and decoupling

//code using error kernel pattern with put the risky task down to leaf
// where is can be restart easily without disruption
// this code assign the risky task of calculate the climb rate
  // to isolate actor if it throw exception, it can be
  // restart by the supervisor
//  case class CalculateAltitude(lastTick: Long,
//                               tick: Long, roc: Double)
//  case class AltitudeCalculated(newTick: Long,
//                                altitude: Double)
//  val altitudeCalculator = context.actorOf(Props(new Actor {
//    def receive = {
//      case CalculateAltitude(lastTick, tick, roc) =>
//        if (roc == 0)
//          throw new ArithmeticException("Divide by zero")
//        val alt = ((tick - lastTick) / 60000.0) *
//          (roc * roc) / roc
//        sender ! AltitudeCalculated(tick, alt)
//    }
//  }), "AltitudeCalculator")
//
//  override val supervisorStrategy =
//    OneForOneStrategy(-1, Duration.Inf) { case _ => Restart}
//def altimeterReceive: Receive = {
//  // Change our rate of climb
//  case RateChange(amount) =>
//    rateOfClimb = amount.min(1.0f).max(-1.0f) *
//      maxRateOfClimb
//  // Ask the altitude calculator to calculate us
//  // a new altitude
//  case Tick =>
//    val tick = System.currentTimeMillis
//    altitudeCalculator ! CalculateAltitude(lastTick,
//      tick,
//      rateOfClimb)
//    lastTick = tick
//  // The calculator has successfully calculated a new
//  // altitude and we can now deal with it
//  case AltitudeCalculated(tick, altdelta) =>
//    altitude += altdelta
//    sendEvent(AltitudeUpdate(altitude))
//}

  //modify the receive method to custom receive
  def receive = eventSourceReceive orElse altimeterReceive
  //the custom receive must indicate return type
  override type Receive = PartialFunction[Any,Unit]

  def altimeterReceive:Receive = {
    //only process mutable data in the receive function only
    //climb rate changed
    case RateChange(amount) =>
      //truncate the range to [-1,1] before multiply
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      log info s"Altimeter changed climb rate to $rateOfClimb."
    // calc new altitude
    case Tick =>
      val tick = System.currentTimeMillis()
      altitude = altitude + ((tick - lastTick)/60000.0) * rateOfClimb
      //send out msg as event
      sendEvent(AltitudeUpdate(altitude))

  }

  // kill the ticker when stop
  override def postStop():Unit = ticker.cancel()
}
