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

package freestyle.free.cache.hashmap

import freestyle.free.cache.KeyValueMap
import freestyle.free.Capture
import java.util.concurrent.ConcurrentHashMap

/* A _mutable_ Concurrent Hash Map, that contains a _mutable_ set of _immutable_
 * keys linked to _immutable_ values.
 *
 *  Being immutable, the hashCode is attached to each key and
 */
final class ConcurrentHashMapWrapper[F[_], Key, Value](
  implicit hasher: Hasher[Key],
  C: Capture[F]
) extends KeyValueMap[F, Key, Value] {

  private[this] final case class HKey(key: Key, hash: Int) {
    override def hashCode() = hash

    override def equals(other: Any): Boolean =
      if (other.isInstanceOf[HKey]) {
        this.key == other.asInstanceOf[HKey].key
      } else false

  }

  private[this] def hkey(key: Key) = new HKey(key, hasher.hashCode(key))

  // table is a _mutable_ concurrent HashMap. It contains
  private[this] val table = new ConcurrentHashMap[this.HKey, Value]()

  /**
    * @returns Some(v) if v is the value to which the specified key is mapped, or
    *          None if this map contains no mapping for the key
    */
  override def get(key: Key): F[Option[Value]] = C.capture {
    Option(table.get(hkey(key))) // Option.apply handles null
  }

  override def put(key: Key, value: Value): F[Unit] = C.capture {
    table.put(hkey(key), value) // Option.apply handles null
    ()
  }

  override def putAll(keyValues: Map[Key, Value]): F[Unit] = C.capture {
    keyValues.foreach{ case (k, v) => table.put(hkey(k), v) }
  }

  override def putIfAbsent(key: Key, newVal: Value): F[Unit] = C.capture {
    table.putIfAbsent(hkey(key), newVal)
    ()
  }

  override def delete(key: Key): F[Unit] = C.capture {
    table.remove(hkey(key)) // Option.apply handles null
    ()
  }

  override def hasKey(key: Key): F[Boolean] =
    C.capture(table.containsKey(hkey(key)))

  override def keys: F[List[Key]] = {
    import scala.collection.JavaConverters._
    C.capture(table.keySet().asScala.toList.map(_.key))
  }

  override def clear: F[Unit] = C.capture(table.clear)

  override def replace(key: Key, newVal: Value): F[Unit] = C.capture {
    table.replace(hkey(key), newVal)
    ()
  }

  override def isEmpty: F[Boolean] = C.capture{
    table.isEmpty
  }

}

object implicits {

  implicit def concurrentHashMapWrapper[F[_], Key, Value](
    implicit hasher: Hasher[Key],
    C: Capture[F]
  ): KeyValueMap[F, Key, Value] =
    new ConcurrentHashMapWrapper[F, Key, Value]()

}
