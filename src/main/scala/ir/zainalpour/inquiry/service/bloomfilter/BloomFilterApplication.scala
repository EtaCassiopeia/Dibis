package ir.sahab.inquiry.service.bloomfilter

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import ir.sahab.inquiry.service.bloomfilter.config.ZooKeeperConfiguration
import org.apache.curator.framework.CuratorFramework

/**
 * <h1>BloomFilterApplication</h1>
 * The BloomFilterApplication 
 *
 * @author  Mohsen Zainalpour
 * @version 1.0
 * @since   5/10/15 
 */

object BloomFilterApplication extends ZooKeeperConfiguration {

  override val config = ConfigFactory.load("common")

  def main(args: Array[String]): Unit = {
    if (args.isEmpty || args.head == "InquiryService")
      startRemoteInquirySystem()
    if (args.isEmpty || args.head == "Lookup")
      startRemoteLookupSystem()
    if (args.isEmpty || args.head == "Client")
      startClient()
  }

  def startRemoteInquirySystem(): Unit = {

    val system = ActorSystem("InquirySystem",
      ConfigFactory.load("inquiryservice"))

    val zkClient: CuratorFramework = initZooKeeperClient()

    val actorRef = system.actorOf(Props(classOf[BloomFilterActor], zkClient), "inquiryService")

    println(actorRef.path.toString)

    println("Started InquirySystem - waiting for messages")
  }


  def startClient(): Unit = {
    /*val system =
      ActorSystem("ProxySystem", ConfigFactory.load("remotelookup"))

    val zkClient: CuratorFramework = initZooKeeperClient()

    val actor = system.actorOf(Props(classOf[ProxyActor], zkClient), "proxyActor")

    println("Started ProxySystem")

    import scala.concurrent.duration._

    implicit val askTimeout = Timeout(5 seconds)
    import system.dispatcher

    val urls = Seq("www.google.com", "www.yahoo.com", "www.facebook.com", "www.github.net")
    system.scheduler.scheduleOnce(1 second) {
      urls.filter(_.contains(".com")).foreach(url => actor ! Add(url))

      Thread.sleep(100)

      urls.foreach { url =>
        val future = actor ? Contains(url)
        future.onSuccess {
          case result => println(s"has $url reported? : $result")
        }
      }
    }*/

    val dibisClient = new DibisClient()

    //Thread.sleep(5000)

    val urls = Seq("www.google.com", "www.yahoo.com", "www.facebook.com", "www.github.net")

    urls.filter(_.contains(".com")).foreach(url => dibisClient.add(url))

    //Thread.sleep(1000)

    urls.foreach { url =>
      println(s"has $url reported? : ${dibisClient.contains(url)}")
    }


  }

  def startRemoteLookupSystem(): Unit = {
    val system =
      ActorSystem("LookupSystem", ConfigFactory.load("remotelookup"))
    val remotePath =
      "akka.tcp://InquirySystem@127.0.0.1:2552/user/inquiryService"
    val actor = system.actorOf(Props(classOf[ExperimentalClientActor], remotePath), "lookupActor")

    println("Started LookupSystem")

    /*
      case class IsReported(url: String)

  case class Report(url: String)
     */

    import scala.concurrent.duration._

    implicit val askTimeout = Timeout(5 seconds)
    import system.dispatcher

    val urls = Seq("www.google.com", "www.yahoo.com", "www.facebook.com", "www.github.net")
    system.scheduler.scheduleOnce(1 second) {
      urls.filter(_.contains(".com")).foreach(url => actor ! Add(url))


      urls.foreach { url =>
        val future = actor ? Contains(url)
        future.onSuccess {
          case result => println(s"has $url reported? : $result")
        }
      }
    }

  }

}
