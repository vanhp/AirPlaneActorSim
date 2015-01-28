//Package vanh.akka.avionics

import akka.actor.{Actor, ActorLogging}
import scala.concurrent.duration._

// the altimeter for the plane
object Altimeter{
  case class RateChange(amount: Float)
}

class Altimeter extends Actor with ActorLogging{
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

  def receive = {
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

  }
  // kill the ticker when stop
  override def postStop():Unit = ticker.cancel()
}
