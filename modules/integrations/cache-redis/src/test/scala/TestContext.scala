/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  override def beforeAll = {
    server.start()
    ()
  }
  override def afterAll = {
    server.stop()
    actorSystem.terminate()
    ()
  }
  override def beforeEach = {
    client.flushdb
    ()
  }
}
