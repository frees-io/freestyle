package freestyle.cache.redis

import _root_.redis.embedded.RedisServer
import _root_.redis.RedisClient
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait RedisTestContext extends BeforeAndAfterAll with BeforeAndAfterEach { self: Suite =>

  private[this] val server: RedisServer = new RedisServer()

  private[this] implicit val actorSystem: ActorSystem =
    ActorSystem.create("testing")
  val client: RedisClient =
    RedisClient(host = "localhost", port = server.getPort)

  override def beforeAll = server.start()
  override def afterAll = {
    server.stop()
    actorSystem.terminate()
  }
  override def beforeEach = client.flushdb
}
