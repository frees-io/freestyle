package freestyle.redis.fscredis

import cats.data.Kleisli
import scredis.serialization.{Reader, Writer}
import scredis.commands.{KeyCommands, ServerCommands, StringCommands}

trait StringCommandsCont {

  import Format.syntax._

  def append[Key, Value](key: Key, value: Value)(
      implicit format: Format[Key],
      writer: Writer[Value]
  ): RawScredisOps[Long] =
    Kleisli((client: StringCommands) => client.append(format(key), value))

  def get[Key, Value](key: Key)(
      implicit format: Format[Key],
      writer: Reader[Value]
  ): RawScredisOps[Option[Value]] =
    Kleisli((client: StringCommands) => client.get[Value](format(key)))

  def set[Key: Format, Value: Writer](key: Key, value: Value): RawScredisOps[Boolean] =
    Kleisli((client: StringCommands) => client.set(key, value))

}

trait KeyCommandsCont {

  def del[Key](keys: Seq[Key])(implicit format: Format[Key]): RawScredisOps[Long] =
    Kleisli((client: KeyCommands) => client.del(keys.map(format): _*))

  def exists[Key](key: Key)(implicit format: Format[Key]): RawScredisOps[Boolean] =
    Kleisli((client: KeyCommands) => client.exists(format(key)))

}

trait ServerCommandsCont {

  def flushDB: RawScredisOps[Unit] =
    Kleisli((client: ServerCommands) => client.flushDB)

}

object ScredisCont extends StringCommandsCont with KeyCommandsCont with ServerCommandsCont
