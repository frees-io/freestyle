package freestyle.cache.hashmap

import freestyle.cache.KeyValueMap
import freestyle.Capture
import java.util.concurrent.ConcurrentHashMap

/* A _mutable_ Concurrent Hash Map, that contains a _mutable_ set of _immutable_
 * keys linked to _immutable values.
 *
 *  Being immutable, the hashCode is attached to each key and
 */
final class ConcurrentHashMapWrapper[F[_], Key, Value](
    implicit hasher: Hasher[Key],
    C: Capture[F]
) extends KeyValueMap[F, Key, Value] {

  private[this] final class HKey(key: Key, hash: Int) {
    override def hashCode() = hash
  }

  private[this] def hkey(key: Key) = new HKey(key, hasher.hashCode(key))

  // table is a _mutable_ concurrent HashMap. It contains
  private[this] val table = new ConcurrentHashMap[this.HKey, Value]()

  /**
   * @returns Some(v) if v is the value to which the specified key is mapped, or
   *   None if this map contains no mapping for the key
   */
  override def get(key: Key): F[Option[Value]] =
    C.capture(Option(table.get(hkey(key)))) // Option.apply handles null

  override def put(key: Key, value: Value): F[Unit] =
    C.capture(table.put(hkey(key), value)) // Option.apply handles null

  override def delete(key: Key): F[Unit] =
    C.capture(table.remove(hkey(key))) // Option.apply handles null

  override def hasKey(key: Key): F[Boolean] =
    C.capture(table.containsKey(hkey(key)))

  override def clear: F[Unit] = C.capture(table.clear)

}

object implicits {

  implicit def concurrentHashMapWrapper[F[_], Key, Value](
      implicit hasher: Hasher[Key],
      C: Capture[F]
  ): KeyValueMap[F, Key, Value] =
    new ConcurrentHashMapWrapper[F, Key, Value]()

}
