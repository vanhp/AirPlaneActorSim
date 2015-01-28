package vanh.akka.avionics

/**
 * Created by vanh on 1/21/15.
 *
 */

import akka.actor.{AllForOneStrategy, OneForOneStrategy, SupervisorStrategy}
import akka.actor.SupervisorStrategy.Decider
import scala.concurrent.duration._

trait SupervisionStrategyFactory {
  def makeStrategy(maxNrRetetries:Int, withinTimeRange:Duration)
                  (decider: Decider):SupervisorStrategy

}
trait OneForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetetries:Int, withinTimeRange:Duration)
                  (decider: Decider):SupervisorStrategy = OneForOneStrategy(maxNrRetetries,withinTimeRange)(decider)
}

trait AllForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetetries:Int, withinTimeRange:Duration)
                  (decider: Decider):SupervisorStrategy = AllForOneStrategy(maxNrRetetries,withinTimeRange)(decider)
}
