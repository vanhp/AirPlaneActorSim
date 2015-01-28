package vanh.akka.avionics

/**
 * Created by vanh on 1/17/15.
 *
 */
import akka.actor.{Props,Actor,ActorSystem,ActorRef}
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