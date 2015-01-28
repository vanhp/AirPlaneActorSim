package vanh.akka.avionics

/**
 * Created by vanh on 1/17/15.
 *
 */

import akka.actor.{Props, Actor, ActorLogging}
import EventSource._
import akka.util.Timeout
import vanh.akka.avionics.IsolatedLifeCycleSupervisor.WaitForStart
import scala.concurrent.duration._
import scala.concurrent.Await

import akka.pattern.ask


object Plane {
  // give control surface any actor that want it
  case object GiveMeControl
 // case class Controls(ctrl:ControlSurface)

}

class Plane extends Actor with ActorLogging{
  this: AltimeterProvider
    with PilotProvider
    with HeadingIndicator
  //  with EventSource
    with LeadFlightAttendantProvider =>

  import Altimeter._
  import Plane._
  import FlightAttendant._

  //can't directly new up the altimeter
  //must use context.actorOf method to create a new
  // actorRef and tie it to this actor as a child

  //create headingIndicator which tie it life to the plane
  val headingIndicator = context.actorOf(Props[HeadingIndicator],"HeadingIndicator")

  //create altimeter which tie it life to the plane
  val altimeter = context.actorOf(Props[Altimeter],"Altimeter")
  //create controlsurface which tie it life to the plane
  val controls = context.actorOf(Props(new ControlSurface(self,headingIndicator,altimeter)),"ControlSurface")

  //create flight crew as child of the plane
  val cfgstr = "vanh.akka.avionics.flightcrew"
  val config = context.system.settings.config
  val pilot = context.actorOf(Props[Pilot],config.getString(s"$cfgstr.pilotName"))
  val copilot = context.actorOf(Props[CoPilot],config.getString(s"$cfgstr.copilotName"))
  val autopilot = context.actorOf(Props[AutoPilot],"autopilot")
  val flightAttendant = context.actorOf(Props[LeadFlightAttendant],config.getString(s"$cfgstr.leadAttendantName"))

  val pilotName = config.getString(s"$cfgstr.pilotName")
  val copilotName = config.getString(s"$cfgstr.copilotName")
  val attendantName = config.getString(s"$cfgstr.leadAttendName")


  // There's going to be a couple of asks below and
  // a timeout is necessary for that.
  implicit val askTimeout = Timeout(1.second)
  def startEquipment() {
    val controls = context.actorOf(
      Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          val alt = context.actorOf(Props(newAltimeter), "Altimeter")
          // These children get implicitly added to the hierarchy
          //  context.actorOf(Props(newAutopilot), "AutoPilot")
          context.actorOf(Props(new ControlSurface(self,headingIndicator,alt)), "ControlSurfaces")
        }
      }), "Equipment")
    Await.result(controls ? WaitForStart, 1.second)
  }
  def actorForControls(name:String) =
    context.actorFor("Equiment/" + name)

  def startPeople() {
    val plane = self
    val controls = actorForControls("ControlSurface")
    val autopilot = actorForControls("AutoPilot")
    val altimeter = actorForControls("Altimeter")
    val people = context.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          // These children get implicitly added to
          // the hierarchy
          context.actorOf(Props(newPilot(plane,autopilot,headingIndicator,altimeter)), pilotName)
          context.actorOf(Props(newCoPilot(plane,altimeter)), copilotName)
        }
      }), "Pilots")
    // Use the default strategy here, which
    // restarts indefinitely
    context.actorOf(Props(newFlightAttendant), attendantName)
    Await.result(people ? WaitForStart, 1.second)
  }

  //help look up the actor within the "Pilots" Supervisor
  def actorForPilots(name:String) =
    context.actorFor("Pilots/"+ name)

  override def preStart() {
    import EventSource.RegisterListener
    import Pilots.ReadyToGo

    startEquipment()
    startPeople()
    //bootstrap system
    actorForControls("Altimeter") ! RegisterListener(self)
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo

//    altimeter ! RegisterListener(self)
//    List(pilot,copilot) foreach { _ ! Pilots.ReadyToGo}
  }

  def receive = {
    // some one ask for control
    case GiveMeControl => log info "Plane giving control."
      sender ! controls // send them the control ref

      //send out msg as event
  //    sendEvent(Controls(controls))

    case AltitudeUpdate(altitude) =>
      log info s"Altitude is now: $altitude"
  }

}
