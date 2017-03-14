package freestyle.cache.redis.rediscala

import cats.data.Kleisli

import scala.concurrent.Future
import _root_.redis.{Cursor, ByteStringDeserializer => Deserializer, ByteStringSerializer => Serializer}
import _root_.redis.commands.{Keys => KeyCommands, Server => ServerCommands, Strings => StringCommands}

private[rediscala] trait StringCommandsCont {

  def append[Key, Value](key: Key, value: Value)(
      implicit format: Format[Key],
      writer: Serializer[Value]
  ): Ops[Future, Long] =
    Kleisli((client: StringCommands) => client.append(format(key), value))

  def get[Key, Value](key: Key)(
      implicit format: Format[Key],
      writer: Deserializer[Value]
  ): Ops[Future, Option[Value]] =
    Kleisli((client: StringCommands) => client.get[Value](format(key)))

  def set[Key: Format, Value: Serializer](key: Key, value: Value): Ops[Future, Boolean] =
    Kleisli((client: StringCommands) => client.set(key, value))

  def mset[Key, Value: Serializer](keyValues: Map[Key, Value])(implicit format: Format[Key]): Ops[Future, Boolean] = {
    val b = keyValues.map { case (k,v) => (format(k), v) }
    Kleisli((client: StringCommands) => client.mset(b))
  }

  def setnx[Key : Format, Value: Serializer](key: Key, value: Value): Ops[Future, Boolean] =
    Kleisli((client: StringCommands) => client.setnx(key, value))

  def setxx[Key : Format, Value: Serializer](key: Key, value: Value): Ops[Future, Boolean] =
    Kleisli((client: StringCommands) => client.set(key, value, XX = true))

}

private[rediscala] trait KeyCommandsCont {

  def del[Key](keys: List[Key])(implicit format: Format[Key]): Ops[Future, Long] =
    Kleisli((client: KeyCommands) => client.del(keys.map(format): _*))

  def exists[Key](key: Key)(implicit format: Format[Key]): Ops[Future, Boolean] =
    Kleisli((client: KeyCommands) => client.exists(format(key)))

  def keys[Key]: Ops[Future, Seq[String]] =
    Kleisli((client: KeyCommands) => client.keys("*"))

  def scan[Key]: Ops[Future, Cursor[Seq[String]]] =
    Kleisli((client: KeyCommands) => client.scan(0, Option(1), None))
}

private[rediscala] trait ServerCommandsCont {

  def flushDB: Ops[Future, Boolean] =
    Kleisli((client: ServerCommands) => client.flushdb)

}

private[rediscala] object RediscalaCont extends StringCommandsCont with KeyCommandsCont with ServerCommandsCont
