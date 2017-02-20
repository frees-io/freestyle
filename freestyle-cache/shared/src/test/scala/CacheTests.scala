package freestyle.cache

import cats.Id
import freestyle.cache.hashmap._
import org.scalatest._

class CacheTests extends WordSpec with Matchers with BeforeAndAfterEach {
  import Hasher.string

  private[this] val wrapper: ConcurrentHashMapWrapper[Id, String, Int] =
    new ConcurrentHashMapWrapper[Id, String, Int]

  override def beforeEach = wrapper.clear

}
