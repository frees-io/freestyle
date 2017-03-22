package freestyle

import cats.implicits._
import cats.Id
import org.scalatest.{Matchers, WordSpec}

class implicitsTests extends WordSpec with Matchers {

  "Implicits" should {

    import algebras._
    import freestyle.implicits._

    "make monad implicitly available" in {
      type G[A] = FreeS[SCtors1.Op, A]
      implicitly[cats.Monad[G]].isInstanceOf[cats.Monad[G]]
    }

    "enable sequenceU" in {
      val s = SCtors1[SCtors1.Op]
      val program = for {
        a <- List(s.x(1), s.x(2)).sequenceU
      } yield a
      program.isInstanceOf[FreeS[SCtors1.Op, List[Int]]] shouldBe true
    }

    "enable sequence" in {
      val s = SCtors1[SCtors1.Op]
      val program = for {
        a <- List(s.x(1), s.x(2)).sequence[λ[a => FreeS[SCtors1.Op, a]], Int]
      } yield a
      program.isInstanceOf[FreeS[SCtors1.Op, List[Int]]] shouldBe true
    }
  }
}
