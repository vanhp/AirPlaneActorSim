package vanh.akka.avionics

/**
 * Created by vanh on 1/21/15.
 */
import akka.actor.Actor

object IsolatedLifeCycleSupervisor {
  //msg to wait for finish starting
  case object WaitForStart
  case object Started
}
trait IsolatedLifeCycleSupervisor extends Actor {
  import IsolatedLifeCycleSupervisor._

  def receive = {
    //done starting
    case WaitForStart =>
      sender ! Started
    case m => //any other msg is an error
      throw new Exception(s"Don't call${self.path.name} directly ($m).")
  }
  //to be implement by subsclass
  def childStarter():Unit

  //these override default are to let children survive parent restart
  //even they survive, they still need to be restart but with their
  //old ActorRef

  //not allow for any modification by subclass inorder to control life cycle
  //start children after started completed
  final override def preStart(): Unit = { childStarter()}
  //skip default by call postRestart instead
  final override def postRestart(reason:Throwable){}
  //avoid stopping children by default
  final override def preRestart(reason:Throwable,message:Option[Any]){}
}
