package freestyle.cache.redis.rediscala

import cats.{~>, Functor}
import cats.syntax.functor._
import scala.concurrent.Future
import _root_.redis.{ByteStringSerializer => Serializer, ByteStringDeserializer => Deserializer}
import freestyle.cache.KeyValueMap

class MapWrapper[M[_], Key, Value](
    implicit format: Format[Key],
    parser: Parser[Key],
    read: Deserializer[Option[Value]],
    writer: Serializer[Value],
    toM: Future ~> M,
    funcM: Functor[M]
) extends KeyValueMap[Ops[M, ?], Key, Value] {

  override def get(key: Key): Ops[M, Option[Value]] =
    RediscalaCont.get[Key, Option[Value]](key).transform(toM).map(_.flatten)

  override def put(key: Key, value: Value): Ops[M, Unit] =
    RediscalaCont.set(key, value).transform(toM).void

  override def delete(key: Key): Ops[M, Unit] =
    RediscalaCont.del(List(key)).transform(toM).void

  override def hasKey(key: Key): Ops[M, Boolean] =
    RediscalaCont.exists(key).transform(toM)

  override def keys: Ops[M, List[Key]] =
    RediscalaCont.keys.transform(toM).map(_.toList.flatMap(parser.apply))

  override def clear(): Ops[M, Unit] =
    RediscalaCont.flushDB.transform(toM).void

}

object MapWrapper {

  implicit def apply[M[_], Key, Value](
      implicit format: Format[Key],
      parser: Parser[Key],
      read: Deserializer[Option[Value]],
      writer: Serializer[Value],
      toM: Future ~> M,
      funcM: Functor[M]
  ): MapWrapper[M, Key, Value] = new MapWrapper
}
