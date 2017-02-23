package freestyle.cache.redis.rediscala

import _root_.redis.{ByteStringSerializer => Serializer, ByteStringDeserializer => Deserializer}

trait Format[A] extends (A ⇒ String)

object Format {

  def apply[A](print: A ⇒ String) = new Format[A] {
    def apply(a: A): String = print(a)
  }

  implicit val string: Format[String] = new Format[String] {
    def apply(str: String): String = str
  }

}

trait Parser[A] extends (String => Option[A])

object Parser {
  def apply[A](parse: String => Option[A]) = new Parser[A] {
    def apply(s: String): Option[A] = parse(s)
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
