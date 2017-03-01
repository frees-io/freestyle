package freestyle.cache.redis.rediscala

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

