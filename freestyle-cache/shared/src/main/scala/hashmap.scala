package freestyle.cache.hashmap

import java.util.concurrent.ConcurrentHashMap

trait Hasher[A] {
  def hashCode(a: A): Int
}

object Hasher {
  def apply[A](fun: A => Int) = new Hasher[A] {
    override def hashCode(a: A): Int = fun(a)
  }
}

object HasherInstances {

  private[this] class ValueHasher[A] extends Hasher[A] {
    def hashCode(s: A): Int = s.##
  }

  implicit val hashDouble: Hasher[Double] = new ValueHasher[Double]
  implicit val hashInt: Hasher[Int]       = new ValueHasher[Int]
  implicit val hashLong: Hasher[Long]     = new ValueHasher[Long]
  implicit val hashString: Hasher[String] = new ValueHasher[String]

}

/* A _mutable_ Concurrent Hash Map, that contains a _mutable_ set of _immutable_
 * keys linked to _immutable values.
 *
 *  Being immutable, the hashCode is attached to each key and
 */
final class ConcurrentHashMapWrapper[Key, Value](implicit hasher: Hasher[Key]) {

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
  def get(key: Key): Option[Value] =
    Option(table.get(hkey(key))) // Option.apply handles null

  def put(key: Key, value: Value): Option[Value] =
    Option(table.put(hkey(key), value)) // Option.apply handles null

  def delete(key: Key): Option[Value] =
    Option(table.remove(hkey(key))) // Option.apply handles null

  def flushAll(): Unit = table.clear

}
