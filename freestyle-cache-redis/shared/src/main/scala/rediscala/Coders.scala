package freestyle.cache.redis.rediscala

import _root_.redis.{ByteStringSerializer => Serializer, ByteStringDeserializer => Deserializer}

trait Format[A] extends (A ⇒ String)

object Format {

  def apply[A](print: A ⇒ String) = new Format[A] {
    def apply(a: A): String = print(a)
  }

  object syntax {
    implicit class KeyFormat[Key](key: Key) {
      def format(implicit FK: Format[Key]): String = FK(key)
    }
  }

}

object Deserializers {

  def parser[A](parser: String ⇒ Option[A]): Deserializer[Option[A]] =
    Deserializer.String.map(parser)

}

object Serializers {

  def printer[A](printer: A ⇒ String): Serializer[A] =
    Serializer.String.contramap(printer)

}
