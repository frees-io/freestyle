package freestyle.cache

import cats.{~>}
import freestyle.cache._
import freestyle.cache.redis.rediscala.{MapWrapper, Ops => RediscalaOps}

package redis {

  class RedisKeyValueProvider[Key, Val] {

    val cache = new KeyValueProvider[Key, Val]

    object implicits {

      implicit def redisCacheInterpreter[M[+ _]](
          implicit redisMap: MapWrapper[M, Key, Val],
          interpret: RediscalaOps[M, ?] ~> M
      ): cache.CacheM.Interpreter[M] =
        new RedisCacheInterpreter[M](redisMap, interpret)

      private[this] class RedisCacheInterpreter[M[+ _]](
          redisMap: MapWrapper[M, Key, Val],
          interpret: RediscalaOps[M, ?] ~> M
      ) extends cache.CacheM.Interpreter[M] {

        override def getImpl(key: Key): M[Option[Val]] =
          interpret(redisMap.get(key))
        override def putImpl(key: Key, newVal: Val): M[Unit] =
          interpret(redisMap.put(key, newVal))
        override def delImpl(key: Key): M[Unit] =
          interpret(redisMap.delete(key))
        override def hasImpl(key: Key): M[Boolean] =
          interpret(redisMap.hasKey(key))
        override def clearImpl: M[Unit] =
          interpret(redisMap.clear)
      }

    }
  }
}

package object redis {

  def apply[Key, Val] = new RedisKeyValueProvider[Key, Val]

}
