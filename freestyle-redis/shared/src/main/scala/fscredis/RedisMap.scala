/*
 */
package freestyle.redis.fscredis

import cats.{~>, Functor}
import cats.syntax.functor._
import scala.concurrent.Future
import scredis.serialization.{Reader, Writer}

class RedisMapWrapper[M[+ _], Key, Value](
    implicit format: Format[Key],
    read: Reader[Option[Value]],
    writer: Writer[Value],
    toM: Future ~> M,
    funcM: Functor[M]
) {

  def get(key: Key): ScredisOps[M, Option[Value]] =
    (comm: ScredisCommands) => {
      toM(comm.get[Option[Value]](format(key))).map(_.flatten)
    }

  def put(key: Key, value: Value): ScredisOps[M, Unit] =
    (comm: ScredisCommands) => {
      toM(comm.set[Value](format(key), value)).void
    }

  def delete(key: Key): ScredisOps[M, Unit] =
    (comm: ScredisCommands) => {
      toM(comm.del(key)).void
    }

  def flushAll(): ScredisOps[M, Unit] =
    (comm: ScredisCommands) => {
      toM(comm.flushAll)
    }

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
