package freestyle

import cats.{~>, Functor}
import cats.data.Kleisli
import freestyle.cache._
import scala.concurrent.Future
import scredis.serialization.{Reader, Writer}
import freestyle.redis.fscredis.{RedisMapWrapper, ScredisOpsInterpret}

package redis {

  class RedisKeyValueProvider[Key, Val] {

    val delegate = new KeyValueProvider[Key, Val]
    import delegate._

    object implicits {
      import fscredis.Format

      class RedisCacheInterpreter[M[+ _]](
          implicit redisMap: RedisMapWrapper[M, Key, Val],
          runner: ScredisOpsInterpret[M]
      ) extends CacheM.Interpreter[M] {

        import fscredis.RedisMapWrapper

        override def getImpl(key: Key): M[Option[Val]] =
          runner(redisMap.get(key))
        override def putImpl(key: Key, newVal: Val): M[Unit] =
          runner(redisMap.put(key, newVal))
        override def delImpl(key: Key): M[Unit] =
          runner(redisMap.delete(key))
        override def hasImpl(key: Key): M[Boolean] =
          runner(redisMap.hasKey(key))
        override def clearImpl: M[Unit] =
          runner(redisMap.clear)

      }
    }
  }
}

package object redis {

  def apply[Key, Val] = new RedisKeyValueProvider[Key, Val]

}
