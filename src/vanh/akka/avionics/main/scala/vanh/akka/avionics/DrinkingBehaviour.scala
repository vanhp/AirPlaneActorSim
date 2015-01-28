package vanh.akka.avionics

import akka.actor.{Props, Actor, ActorRef}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
 * Created by vanh on 1/27/15.
 * To provide erratic behaviour to pilot
 * by sending random drink level
 */
object DrinkingBehaviour {
  //internal msg blood alcohol level
  case class LevelChanged(level:Float)
  //outbound msg
  case object FeelingSober
  case object FeelingTipsy
  case object FeelingLikeZaphod

  //factory method to instantiate
  def apply(drinker: ActorRef) = {
    new DrinkingBehaviour(drinker) with DrinkingResolution

//    def props(drinker: ActorRef):Props = {
//      new Props( DrinkingBehaviour(drinker) with DrinkingResolution)
//  }
}
//for help in testing
trait DrinkingProvider{
 def newDrinkingBehaviour(drinker:ActorRef):Props =
    Props(DrinkingBehaviour(drinker))
}

trait DrinkingResolution{
  import scala.util.Random
  def initialSobering: FiniteDuration = 1.second
  def soberingInterval: FiniteDuration = 1.second
  def drinkInterval(): FiniteDuration = Random.nextInt(300).second
}

class DrinkingBehaviour(drinker:ActorRef) extends Actor {
  this: DrinkingResolution =>
  import DrinkingBehaviour._

  implicit val ec = context.dispatcher
  var currentLevel = 0f
  val scheduler = context.system.scheduler
  val sobering = scheduler.schedule(initialSobering,soberingInterval,self,LevelChanged(-0.0001f))
  //stop the timer when shutdown
  override def postStop(): Unit ={sobering.cancel()}
  override def preStart(): Unit ={drink()}
  //blood alcohol increase one level with each call
  // it's ok to not clean up only a msg is sent to DLO
  def drink() = scheduler.scheduleOnce(drinkInterval(),self,LevelChanged(0.005f))
  def receive = {
    case LevelChanged(amount) =>
      currentLevel = (currentLevel + amount).max(0f)
        drinker ! (if(currentLevel <= 0.01) {
        drink()
        FeelingSober
      } else if(currentLevel <= 0.03) {
        drink()
        FeelingTipsy
      }
        else FeelingLikeZaphod)
  }
}