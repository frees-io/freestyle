package freestyle.cache

import cats.arrow.FunctionK
import cats.{~>, Applicative, Id}
import freestyle.cache.hashmap._
import org.scalatest.{BeforeAndAfterEach, Suite}

trait CacheTestContext extends BeforeAndAfterEach { self: Suite =>

  import Hasher.string
  import freestyle.Capture.freeStyleIdCaptureInstance

  private[this] implicit val rawMap: KeyValueMap[Id, String, Int] =
    new ConcurrentHashMapWrapper[Id, String, Int]

  private[this] implicit val idInt: Id ~> Id = FunctionK.id[Id]

  protected[this] final val provider = new KeyValueProvider[String, Int]

  protected[this] implicit val interpret: provider.CacheM.Interpreter[Id] =
    provider.implicits.cacheInterpreter(rawMap, idInt)

  override def beforeEach = rawMap.clear

}
