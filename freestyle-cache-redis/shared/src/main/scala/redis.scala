package freestyle.cache

import freestyle.cache._
import freestyle.cache.redis.scredis.{RedisMapWrapper, ScredisOpsInterpret}

package redis {

  class RedisKeyValueProvider[Key, Val] {

    val delegate = new KeyValueProvider[Key, Val]
    import delegate._

    object implicits {

      class RedisCacheInterpreter[M[+ _]](
          implicit redisMap: RedisMapWrapper[M, Key, Val],
          runner: ScredisOpsInterpret[M]
      ) extends CacheM.Interpreter[M] {

        import scredis.RedisMapWrapper

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
