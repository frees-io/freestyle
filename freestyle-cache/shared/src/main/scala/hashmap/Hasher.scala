package freestyle.cache.hashmap

trait Hasher[A] {
  def hashCode(a: A): Int
}

object Hasher {
  def apply[A](fun: A => Int) = new Hasher[A] {
    override def hashCode(a: A): Int = fun(a)
  }

  class ValueHasher[A] extends Hasher[A] {
    def hashCode(s: A): Int = s.##
  }

  implicit val double: Hasher[Double] = new ValueHasher[Double]
  implicit val int: Hasher[Int]       = new ValueHasher[Int]
  implicit val long: Hasher[Long]     = new ValueHasher[Long]
  implicit val string: Hasher[String] = new ValueHasher[String]

}
