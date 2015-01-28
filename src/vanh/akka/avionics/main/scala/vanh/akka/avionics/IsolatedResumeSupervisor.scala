package vanh.akka.avionics

//import org.scalatest.tools.DashboardReporter.TestRecord.Duration

import akka.actor.{ActorKilledException, ActorInitializationException}
import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}

import scala.concurrent.duration.Duration

/**
 * Created by vanh on 1/21/15.
 *
 */

abstract class IsolatedResumeSupervisor(maxNrRetries: Int = -1, withinTimeRange:Duration = Duration.Inf) extends
                    IsolatedLifeCycleSupervisor{
  this: SupervisionStrategyFactory =>

  override val supervisorStrategy = makeStrategy(maxNrRetries,withinTimeRange){
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Resume //task relate exception may be resumable
    case _ => Escalate //don't know what to do pass on to higher up

  }
}

abstract class IsolatedStopSupervisor(maxNrRetries: Int = -1, withinTimeRange:Duration = Duration.Inf)
        extends IsolatedLifeCycleSupervisor {
  this: SupervisionStrategyFactory =>

  override val supervisorStrategy = makeStrategy(maxNrRetries,withinTimeRange){
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Stop //every thing
    case _ => Escalate //don't know what to do pass on to higher up

  }
}