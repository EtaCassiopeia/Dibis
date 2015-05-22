package ir.sahab.inquiry.service.bloomfilter

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import ir.sahab.inquiry.service.bloomfilter.config.ZooKeeperConfiguration
import org.apache.curator.framework.CuratorFramework

import scala.concurrent.Await
import scala.concurrent.duration._


/**
 * <h1>DibisClient</h1>
 * The DibisClient 
 *
 * @author  Mohsen Zainalpour
 * @version 1.0
 * @since   5/10/15 
 */
class DibisClient extends ZooKeeperConfiguration {

  override val config = ConfigFactory.load("common")

  val system =
    ActorSystem("ProxySystem", ConfigFactory.load("remotelookup"))

  val zkClient: CuratorFramework = initZooKeeperClient()

  val actor = system.actorOf(Props(classOf[ProxyActor], zkClient).withMailbox("my-custom-mailbox"), "proxyActor")

  implicit val askTimeout = Timeout(5 seconds)

  def add(url: String): Unit = {
    actor ! Add(url)
  }

  def contains(url: String): Boolean = {
    val future = actor ? Contains(url)
    val result = Await.result(future, 5 second)
    if (result == null) {
      return false
    }
    return result.asInstanceOf[Boolean]
  }

}
