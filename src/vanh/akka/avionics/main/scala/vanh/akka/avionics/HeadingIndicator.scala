package vanh.akka.avionics

import akka.actor.{Actor, ActorLogging}
import scala.concurrent.duration._


/**
 * Created by vanh on 1/26/15.
 *
 */

trait HeadingIndicatorProvider extends Actor {
  def newHeadingIndicator:Actor = HeadingIndicator()

}

object HeadingIndicator {
  //indicate direction change
  case class BankChange(amount:Float)
  //send out msg of heading change
  case class HeadingUpdate(heading:Float)
  //change the factory method
  def apply() = new HeadingIndicator with ProductionEventSource
}

trait HeadingIndicator extends Actor with ActorLogging {
  this: EventSource =>
  import HeadingIndicator._
  import context._

  //msg to trigger recalculate heading
  case object Tick
  //max degree/second that plane can move
  val maxDegPerSec = 5
  //timer to schedule the update
  val ticker = system.scheduler.schedule(100.millis,100.millis,self,Tick)

  // last tick for calc change
  var lastTick:Long = System.currentTimeMillis()
  // current rate of bank
  var rateOfBank = 0f
  //current direction
  var heading = 0f

  def headingIndicatorReceive:Receive = {
    // rate change within [-1,1]
    case BankChange(amount) =>
      rateOfBank = amount.min(1.0f).max(-1.0f)
    //calc heading delta from rateofchange,time delta from last calc
    // and max deg/sec
    case Tick =>
      val tick = System.currentTimeMillis()
      val timeDelta = (tick - lastTick) /1000f
      val degs = rateOfBank * maxDegPerSec
      heading = (heading + (360 + (timeDelta * degs))) % 360
      lastTick = tick
      //send out the new heading msg as event
      sendEvent(HeadingUpdate(heading))

  }
  //because of EventSource mixin must compose the receive partial func
  def receive = eventSourceReceive orElse headingIndicatorReceive
  // stop the timer when shutdown
  override def postStop():Unit = ticker.cancel()
}