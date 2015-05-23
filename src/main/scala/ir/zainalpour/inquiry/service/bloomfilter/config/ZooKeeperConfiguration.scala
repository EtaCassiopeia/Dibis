package ir.sahab.inquiry.service.bloomfilter.config

import org.apache.curator.RetryPolicy
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry

/**
 * <h1>ZooKeeperConfiguration</h1>
 * The ZooKeeperConfiguration 
 *
 * @author  Mohsen Zainalpour
 * @version 1.0
 * @since   5/10/15 
 */


/**
 * Provides abilities to use ZooKeeper ensemble as a remote configuration holder.
 */
trait ZooKeeperConfiguration {

  val config: com.typesafe.config.Config

  /**
   * Default ZooKeeper client's connection string.
   */
  protected lazy val DefaultConnectionString: String = config.getString("zookeeper.connectionString")

  /**
   * Default ZooKeeper client's connection timeout.
   */
  protected lazy val DefaultConnectionTimeout: Int = config.getInt("zookeeper.connectionTimeout")

  /**
   * Default ZooKeeper client's session timeout.
   */
  protected lazy val DefaultSessionTimeout: Int = config.getInt("zookeeper.sessionTimeout")

  /**
   * Default number of connection retries to Zookeeper ensemble.
   */
  protected lazy val RetryAttemptsCount: Int = config.getInt("zookeeper.retryAttempts")

  /**
   * Default interval between connection retries to Zookeeper ensemble.
   */
  protected lazy val RetryInterval: Int = config.getInt("zookeeper.retryInterval")

  /**
   * Default retry policy.
   */
  protected lazy val DefaultRetryPolicy: RetryPolicy = new ExponentialBackoffRetry(RetryInterval, RetryAttemptsCount)

  /**
   * Lookup client timeout.
   */
  private val LookupClientTimeout: Int = 15000

  /**
   * Creates ZooKeeper remote configuration client.
   *
   * Initializes connection to the ZooKeeper ensemble.
   *
   * @param connectionString connection string; default value is taken from local configuration
   * @param connectionTimeout connection timeout; default value is taken from local configuration
   * @param sessionTimeout session timeout; default value is taken from local configuration
   * @param retryPolicy connection retry policy; default policy retries specified number of times with increasing
   *                    sleep time between retries
   * @param authScheme authentication scheme; null by default
   * @param authData authentication data bytes; null by default
   * @return client instance
   */
  def initZooKeeperClient(connectionString: String = DefaultConnectionString,
                          connectionTimeout: Int = DefaultConnectionTimeout,
                          sessionTimeout: Int = DefaultSessionTimeout,
                          retryPolicy: RetryPolicy = DefaultRetryPolicy,
                          authScheme: String = null,
                          authData: Array[Byte] = null): CuratorFramework = {
    /*val lookupClient = CuratorFrameworkFactory.builder()
      .connectString(connectionString)
      .retryPolicy(new retry.RetryOneTime(RetryInterval))
      .buildTemp(LookupClientTimeout, TimeUnit.MILLISECONDS)
    val serviceConfigPath = "/system"
    try {
      lookupClient.inTransaction().check().forPath(serviceConfigPath).and().commit()
    } catch {
      case ke: KeeperException => {
        throw new MissingResourceException("Remote configuration is unavailable: %s - %s."
          .format(ke.code(), ke.getMessage), "ZNode", serviceConfigPath)
      }
    }*/

    val client = CuratorFrameworkFactory.builder()
      .connectString(connectionString)
      .connectionTimeoutMs(connectionTimeout)
      .sessionTimeoutMs(sessionTimeout)
      .retryPolicy(retryPolicy)
      //.namespace("system")
      .build()

    try {
      client.start()
      client
    } catch {
      case t: Throwable =>
        throw new RuntimeException("Unable to start ZooKeeper remote configuration client: %s".
          format(t.getLocalizedMessage), t)
    }
  }
}