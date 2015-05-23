package ir.sahab.inquiry.service.bloomfilter

import akka.actor._
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.ask

/**
 * <h1>ExperimentalClientActor</h1>
 * The ExperimentalClientActor 
 *
 * @author  Mohsen Zainalpour
 * @version 1.0
 * @since   5/10/15 
 */
class ExperimentalClientActor (path: String) extends Actor {

  sendIdentifyRequest()
  implicit val askTimeout = Timeout(5 seconds)

  def sendIdentifyRequest(): Unit = {
    context.actorSelection(path) ! Identify(path)
    // context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
  }

  def receive = identifying

  def identifying: Actor.Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      println(s"Remote actor is available: $path")
      context.watch(actor)
      context.become(active(actor), discardOld = true)
    case ActorIdentity(`path`, None) => println(s"Remote actor not available: $path")
    case ReceiveTimeout => sendIdentifyRequest()
    case _ => println("Not ready yet")
  }

  def active(actor: ActorRef): Actor.Receive = {
    case Add(key) => actor ! Add(key)

    case Contains(key) => {

      import context.dispatcher
      val currentSender = sender

      /*val origin = sender
      actor ? IsReported(key) andThen {
        case Success(_) => println(s"got result"); origin ! true
        case Failure(_) => println("failed "); origin ! false
      }*/

      val future: Future[Boolean] = ask(actor, Contains(key)).mapTo[Boolean]
      future.onSuccess {
        case result: Boolean => println(s"got result $result"); currentSender ! result
      }

      future onFailure {
        case msg => println("failed " + msg)
      }
    }

    case msg => println(msg)

  }
}
