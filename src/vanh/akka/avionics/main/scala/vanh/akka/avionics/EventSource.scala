package vanh.akka.avionics

import akka.actor.{Actor, ActorRef}

/**
 * Created by vanh on 1/20/15.
 *
 */

object EventSource{
  //allows actor to register/unregister for event
  case class RegisterListener(listener:ActorRef)
  case class UnregisterListener(listener:ActorRef)
}

trait EventSource {
  //this: Actor =>
  def sendEvent[T](event: T):Unit
  def eventSourceReceive: Actor.Receive

}

trait ProductionEventSource extends EventSource {
  this:Actor =>

  import EventSource._

  //store all listeners in the vector
  var Listeners = Vector.empty[ActorRef]

  //send event to all listeners
  def sendEvent[T](event: T): Unit = Listeners foreach {
    _ ! event
  }

  //special partial function to response to msg for the listeners
  def eventSourceReceive: Receive = {
    case RegisterListener(listener) =>
      Listeners = Listeners :+ listener
    case UnregisterListener(listener) =>
      Listeners = Listeners filter { _ != Listeners }
  }
}
