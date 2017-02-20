package freestyle.redis

import _root_.redis.embedded.RedisServer
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import scredis.{Client ⇒ ScredisClient}

trait RedisTestContext extends BeforeAndAfterAll with BeforeAndAfterEach { self: Suite =>

  private[this] val server: RedisServer = new RedisServer()

  private[this] implicit val actorSystem: ActorSystem =
    ActorSystem.create("testing")
  val client: ScredisClient =
    ScredisClient(host = "localhost", port = server.getPort)

  override def beforeAll = server.start()
  override def afterAll = {
    server.stop()
    actorSystem.terminate()
  }
  override def beforeEach = client.flushAll
}
