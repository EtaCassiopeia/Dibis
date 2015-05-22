package ir.sahab.inquiry.service.bloomfilter

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.{ChildData, PathChildrenCache, PathChildrenCacheEvent, PathChildrenCacheListener}
import org.apache.zookeeper.CreateMode

import scala.concurrent.{TimeoutException, Future}
import scala.concurrent.duration._

/**
 * <h1>ProxyActor</h1>
 * The ProxyActor 
 *
 * @author  Mohsen Zainalpour
 * @version 1.0
 * @since   5/10/15 
 */
class ProxyActor(implicit val zkClient: CuratorFramework) extends Actor with ActorLogging with Stash {

  var replicasActorRef = Set.empty[ActorRef]

  var currentReplicaIndex = 0

  val pathCache = new PathChildrenCache(zkClient, "/dibis/replicas", true)
  pathCache.getListenable.addListener(new PathChildrenCacheListener {
    val selfPath = akka.serialization.Serialization.serializedActorPath(self)

    override def childEvent(curatorFramework: CuratorFramework, pathChildrenCacheEvent: PathChildrenCacheEvent): Unit = {
      //TODO check event type
      val list: java.util.List[ChildData] = pathCache.getCurrentData

      for (i <- 0 to list.size()) {
        val path = new String(list.get(i).getData)
        if (!path.equals(selfPath)) {
          context.actorSelection(path.substring(0, path.indexOf('#'))) ! Identify(path)
          println("Send identify message to : " + path.substring(0, path.indexOf('#')))
        }
      }
    }
  })


  override def postStop() {
    log.info("BloomFilterActor has been terminated.")
    pathCache.close()
    zkClient.close()
  }

  override def preStart() {
    log.info("BloomFilterActor is running .")

    zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath("/dibis/clients/client", akka.serialization.Serialization.serializedActorPath(self).getBytes())
    pathCache.start()

  }

  implicit val askTimeout = Timeout(5 seconds)

  def receive: Actor.Receive = {

    case ActorIdentity(msg, Some(actor)) => {
      context.watch(actor)
      replicasActorRef += actor
      println("#Identift message received : " + msg)
      println(s"#replicasActorRef size = ${replicasActorRef.size}")
    }

    case Terminated(actor) => {
      replicasActorRef = replicasActorRef.filter(_ != actor)
      println("Actor has been terminated : " + actor.path)
    }

    case Add(key) if replicasActorRef.size > 0 => getActor() ! Add(key)

    case Contains(key) if replicasActorRef.size > 0 => {

      import context.dispatcher
      val currentSender = sender

      val future: Future[Boolean] = ask(getActor(), Contains(key)).mapTo[Boolean]
      future.onSuccess {
        case result: Boolean => println(s"got result $result"); currentSender ! result
      }

      future onFailure {
        case msg => println("failed " + msg)
      }
    }

    case msg if replicasActorRef.size == 0 => {
      stash()
      context.become({

        case ActorIdentity(msg, Some(actor)) => {
          context.watch(actor)
          replicasActorRef += actor
          context.unbecome()
          unstashAll()
          println("Identift message received : " + msg)
          println(s"replicasActorRef size = ${replicasActorRef.size}")
        }

        case Terminated(actor) => {
          replicasActorRef = replicasActorRef.filter(_ != actor)
          println("Actor has been terminated : " + actor.path)
        }

        case msg => stash()

      }, discardOld = false)
    }
    //TODO add messaged to queue to send latter
    case _ => {
      println("Not ready yet")
    }
  }

  def getActor(): ActorRef = {
    if (currentReplicaIndex < replicasActorRef.size - 1) {
      currentReplicaIndex += 1
    } else {
      currentReplicaIndex = 0
    }

    replicasActorRef.toList(currentReplicaIndex)
  }
}
