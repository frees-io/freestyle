/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle.cache.redis.rediscala

import cats.{~>, Functor}
import cats.syntax.functor._
import scala.concurrent.Future
import _root_.redis.{ByteStringDeserializer, ByteStringSerializer, Cursor}
import freestyle.cache.KeyValueMap

class MapWrapper[M[_], Key, Value](
    implicit formatKey: Format[Key],
    parseKey: Parser[Key],
    formatVal: Format[Value],
    parseVal: Parser[Value],
    toM: Future ~> M,
    funcM: Functor[M]
) extends KeyValueMap[Ops[M, ?], Key, Value] {

  private[this] implicit val serial: ByteStringDeserializer[Option[Value]] =
    ByteStringDeserializer.String.map(parseVal)
  private[this] implicit val deserial: ByteStringSerializer[Value] =
    ByteStringSerializer.String.contramap(formatVal)

  override def get(key: Key): Ops[M, Option[Value]] =
    RediscalaCont.get[Key, Option[Value]](key).transform(toM).map(_.flatten)

  override def put(key: Key, value: Value): Ops[M, Unit] =
    RediscalaCont.set(key, value).transform(toM).void

  override def putAll(keyValues: Map[Key, Value]): Ops[M, Unit] =
    RediscalaCont.mset(keyValues).transform(toM).void

  override def putIfAbsent(key: Key, newVal: Value): Ops[M, Unit] =
    RediscalaCont.setnx(key, newVal).transform(toM).void

  override def delete(key: Key): Ops[M, Unit] =
    RediscalaCont.del(List(key)).transform(toM).void

  override def hasKey(key: Key): Ops[M, Boolean] =
    RediscalaCont.exists(key).transform(toM)

  override def keys: Ops[M, List[Key]] =
    RediscalaCont.keys.transform(toM).map(_.toList.flatMap(parseKey.apply))

  override def clear(): Ops[M, Unit] =
    RediscalaCont.flushDB.transform(toM).void

  override def replace(key: Key, newVal: Value): Ops[M, Unit] =
    RediscalaCont.setxx(key, newVal).transform(toM).void

  override def isEmpty : Ops[M, Boolean] =
    RediscalaCont.scan.transform(toM).map(_.data.isEmpty)

}

object MapWrapper {

  implicit def apply[M[_], Key, Value](
      implicit formatKey: Format[Key],
      parseKey: Parser[Key],
      formatValue: Format[Value],
      parseValue: Parser[Value],
      toM: Future ~> M,
      funcM: Functor[M]
  ): MapWrapper[M, Key, Value] = new MapWrapper
}
