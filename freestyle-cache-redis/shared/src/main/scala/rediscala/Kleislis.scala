package freestyle.cache.redis.rediscala

import cats.data.Kleisli
import scala.concurrent.Future
import _root_.redis.{ByteStringSerializer => Serializer, ByteStringDeserializer => Deserializer}
import _root_.redis.commands.{
  Keys => KeyCommands,
  Server => ServerCommands,
  Strings => StringCommands
}

trait StringCommandsCont {

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

}

trait KeyCommandsCont {

  def del[Key](keys: Seq[Key])(implicit format: Format[Key]): Ops[Future, Long] =
    Kleisli((client: KeyCommands) => client.del(keys.map(format): _*))

  def exists[Key](key: Key)(implicit format: Format[Key]): Ops[Future, Boolean] =
    Kleisli((client: KeyCommands) => client.exists(format(key)))

  def keys[Key]: Ops[Future, Seq[String]] =
    Kleisli((client: KeyCommands) => client.keys("*"))
}

trait ServerCommandsCont {

  def flushDB: Ops[Future, Boolean] =
    Kleisli((client: ServerCommands) => client.flushdb)

}

object RediscalaCont extends StringCommandsCont with KeyCommandsCont with ServerCommandsCont
