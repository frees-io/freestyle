package freestyle.cache.redis.scredis

import cats.{~>, Functor}
import cats.syntax.functor._
import scala.concurrent.Future
import _root_.scredis.serialization.{Reader, Writer}

class RedisMapWrapper[M[+ _], Key, Value](
    implicit format: Format[Key],
    read: Reader[Option[Value]],
    writer: Writer[Value],
    toM: Future ~> M,
    funcM: Functor[M]
) {

  def get(key: Key): ScredisOps[M, Option[Value]] =
    ScredisCont.get[Key, Option[Value]](key).transform(toM).map(_.flatten)

  def put(key: Key, value: Value): ScredisOps[M, Unit] =
    ScredisCont.set(key, value).transform(toM).void

  def delete(key: Key): ScredisOps[M, Unit] =
    ScredisCont.del(Seq(key)).transform(toM).void

  def hasKey(key: Key): ScredisOps[M, Boolean] =
    ScredisCont.exists(key).transform(toM)

  def clear(): ScredisOps[M, Unit] =
    ScredisCont.flushDB.transform(toM)

}

object RedisMapWrapper {

  implicit def apply[M[+ _], Key, Value](
      implicit format: Format[Key],
      read: Reader[Option[Value]],
      writer: Writer[Value],
      toM: Future ~> M,
      funcM: Functor[M]
  ): RedisMapWrapper[M, Key, Value] = new RedisMapWrapper
}
