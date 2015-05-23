package ir.sahab.inquiry.service.bloomfilter

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import ir.sahab.inquiry.bloomfilter.InfiniteBloomFilter
import ir.sahab.inquiry.service.bloomfilter.config.ZooKeeper
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache._
import org.apache.zookeeper.CreateMode

/**
 * <h1>BloomFilterActor</h1>
 * The BloomFilterActor
 *
 * @author  Mohsen Zainalpour
 * @version 1.0
 * @since   5/9/15 
 */

final case class Store(key: String)

final case class Add(key: String)

final case class Propagate(key: String)

final case class Contains(key: String)

class StorageException(msg: String) extends RuntimeException(msg)

class ServiceUnavailable(msg: String) extends RuntimeException(msg)

case class CuratorConfig(zkConnect: String, zkMaxRetry: Int = 100, baseSleepTimeMs: Int = 100, maxSleepTimeMs: Int = 1000)


class BloomFilterActor(implicit val zkClient: CuratorFramework) extends Actor with ActorLogging with ZooKeeper {


  val bf = BloomFilter

  val storageService = context.actorOf(Props[StorageActor], name = "StorageActor")

  var replicasActorRef = Set.empty[ActorRef]

  val pathCache = new PathChildrenCache(zkClient, "/dibis/replicas", true)
  pathCache.getListenable.addListener(new PathChildrenCacheListener {
    val selfPath = akka.serialization.Serialization.serializedActorPath(self)

    override def childEvent(curatorFramework: CuratorFramework, pathChildrenCacheEvent: PathChildrenCacheEvent): Unit = {
      //TODO check event type
      val list: java.util.List[ChildData] = pathCache.getCurrentData

      for (i <- 0 to list.size()) {
        val path = new String(list.get(i).getData)
        if (!path.equals(selfPath)) {
          println("Send identify message to : " + path.substring(0, path.indexOf('#')))
          context.actorSelection(path.substring(0, path.indexOf('#'))) ! Identify(path)
        }
      }
    }
  })


  override val supervisorStrategy = OneForOneStrategy() {
    case _: ServiceUnavailable => Stop
  }

  override def postStop() {
    log.info("BloomFilterActor has been terminated.")
    pathCache.close()
    zkClient.close()
  }

  override def preStart() {
    log.info("BloomFilterActor is running .")
    initStorage()

    zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath("/dibis/replicas/inquiryService", akka.serialization.Serialization.serializedActorPath(self).getBytes())

    pathCache.start()
  }

  def initStorage() {
  }

  override def receive: Receive = {
    case Add(key) => {
      bf.add(key)
      //storageService ! Store(key)
      replicasActorRef foreach { replica =>
        replica ! Propagate(key)
      }
    }

    case Propagate(key) => {
      bf.add(key)
      println("Propagate message received")
    }

    case Contains(key) => {
      sender ! bf.contains(key)
      println(s"Contains message received[$key]")
    }


    case ActorIdentity(msg, Some(actor)) => {
      context.watch(actor)
      replicasActorRef += actor
      println("Identift message received : " + msg)
      println(s"replicasActorRef size = ${replicasActorRef.size}")
    }

    case Terminated(actor) => {
      replicasActorRef = replicasActorRef.filter(_ != actor)
      println("Actor has been terminated : " + actor.path)
    }

  }

}

object BloomFilter {

  private val infiniteBloomFilter = InfiniteBloomFilter[String](100, 0.0000001)

  @throws(classOf[StorageException])
  def add(key: String): Unit = synchronized {
    infiniteBloomFilter.add(key)
    println("Key added : " + key)
  }

  @throws(classOf[StorageException])
  def contains(key: String): Boolean = synchronized {
    infiniteBloomFilter.contains(key)
  }
}

class StorageActor extends Actor {
  override def receive: Actor.Receive = {
    //TODO Store url in the HBase table
    case Store(key) => {}
  }
}