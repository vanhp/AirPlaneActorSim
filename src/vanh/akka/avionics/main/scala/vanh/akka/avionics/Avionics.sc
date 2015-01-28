
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
  val ec = context.dispatcher

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
    // calc new altitude+
    case Tick =>
      val tick = System.currentTimeMillis()
      altitude = altitude + ((tick - lastTick)/60000.0) * rateOfClimb

  }
  // kill the ticker when stop
  override def postStop():Unit = ticker.cancel()
}


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





import akka.actor._
import akka.pattern.ask
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._


//create thread from global instance
//to run the future from ask package
import scala.concurrent.ExecutionContext.Implicits.global

object Avionics{
  implicit val timeout = Timeout(5.seconds)
  val system = ActorSystem("PlaneSimulation")
  val plane = system.actorOf(Props[Plane],"Plane")

  def main (args: Array[String]) {
    // grab the control
    val control = Await.result((plane ? Plane.GiveMeControl).mapTo[ActorRef],5.seconds)
    // take off
    system.scheduler.scheduleOnce(200.millis) {
      control ! ControlSurface.StickBack(1f)
    }
    //level out
    system.scheduler.scheduleOnce(1.seconds){
      control ! ControlSurface.StickBack(0f)
    }
    //climbing
    system.scheduler.scheduleOnce(3.seconds){
      control ! ControlSurface.StickBack(0.5f)
    }
    //level out
    system.scheduler.scheduleOnce(4.seconds){
      control ! ControlSurface.StickBack(0f)
    }
    //shutdown
    system.scheduler.scheduleOnce(5.seconds){
      system.shutdown()
    }
  }
}