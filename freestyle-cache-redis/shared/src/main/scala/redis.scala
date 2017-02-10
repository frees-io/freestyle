package freestyle.cache

import cats.{~>, Functor}
import freestyle.cache._
import freestyle.cache.redis.scredis.Format
import scala.concurrent.Future
import scredis.serialization.{Reader, Writer}

package redis {

  class RedisKeyValueProvider[Key, Val] {

    val delegate = new KeyValueProvider[Key, Val]
    import delegate._

    object implicits {

      class RedisCacheInterpreter[M[_]](
          implicit form: Format[Key],
          read: Reader[Val],
          writer: Writer[Val],
          fromFut: Future ~> M,
          funcM: Functor[M]
      ) extends CacheM.Interpreter[M] {

        override def getImpl(key: Key): M[Option[Val]] =
          ???
        override def putImpl(key: Key, newVal: Val): M[Unit] =
          ???
        override def delImpl(key: Key): M[Unit] =
          ???
        override def hasImpl(key: Key): M[Boolean] =
          ???
        override def clearImpl: M[Unit] =
          ???

      }
    }
  }
}

package object redis {
  def apply[Key, Val] = new RedisKeyValueProvider[Key, Val]

}
