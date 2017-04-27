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

package freestyle.cache

import cats.arrow.FunctionK
import cats.{~>, Applicative, Id}
import freestyle.FSHandler
import freestyle.cache.hashmap._
import org.scalatest.{BeforeAndAfterEach, Suite}

trait CacheTestContext extends BeforeAndAfterEach { self: Suite =>

  import Hasher.string
  import freestyle.Capture.freeStyleIdCaptureInstance

  private[this] implicit val rawMap: KeyValueMap[Id, String, Int] =
    new ConcurrentHashMapWrapper[Id, String, Int]

  private[this] implicit val idHandler: FSHandler[Id, Id] = FunctionK.id[Id]

  protected[this] final val provider = new KeyValueProvider[String, Int]

  protected[this] implicit val interpret: provider.CacheM.Handler[Id] =
    provider.implicits.cacheHandler(rawMap, idHandler)

  override def beforeEach = rawMap.clear

}
