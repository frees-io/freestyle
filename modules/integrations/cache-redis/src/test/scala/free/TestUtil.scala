/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.free.cache.redis

import cats.arrow.FunctionK
import cats.instances.future
import scala.concurrent.{ExecutionContext, Future}
import freestyle.free.cache.redis.rediscala._

object TestUtil {

  def redisMap(implicit ec: ExecutionContext): MapWrapper[Future, String, Int] =
    new MapWrapper()(
      formatKey = Format((key: String) => key),
      parseKey = Parser((str: String) => Some(str)),
      formatVal = Format((age: Int) => age.toString),
      parseVal = Parser((str: String) => scala.util.Try(Integer.parseInt(str)).toOption),
      toM = FunctionK.id[Future],
      funcM = future.catsStdInstancesForFuture(ec)
    )

}
