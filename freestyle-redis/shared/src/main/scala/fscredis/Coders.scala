package freestyle.redis.fscredis

import scredis.serialization.{Reader, UTF8StringReader, UTF8StringWriter, Writer}

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

object Readers {

  def parser[A](parser: String ⇒ Option[A]): Reader[Option[A]] =
    new ParseStringReader[A](parser)

}

object Writers {

  def printer[A](printer: A ⇒ String): Writer[A] = new PrinterReader[A](printer)

}

class ParseStringReader[A](val parse: String ⇒ Option[A]) extends Reader[Option[A]] {
  override protected def readImpl(bytes: Array[Byte]): Option[A] =
    parse(UTF8StringReader.read(bytes))
}

class PrinterReader[A](val print: A ⇒ String) extends Writer[A] {
  override protected def writeImpl(elem: A): Array[Byte] =
    UTF8StringWriter.write(print(elem))
}
