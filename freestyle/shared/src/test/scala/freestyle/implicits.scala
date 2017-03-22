package freestyle

import cats.implicits._
import cats.Monad
import org.scalatest.{Matchers, WordSpec}

class implicitsTests extends WordSpec with Matchers {

  "Implicits" should {

    import algebras._
    import freestyle.implicits._

    "provide a Monad for FreeS" in {
      type G[A] = FreeS[SCtors1.Op, A]
      "Monad[G]" should compile
    }

    "enable traverseU" in {
      implicit val optionHandler = interps.optionHandler1
      val s = SCtors1[SCtors1.Op]
      val program = List(1, 2).traverseU(s.x)
      program.exec[Option] shouldBe (Some(List(1, 2)))
    }

    "enable sequence" in {
      implicit val optionHandler = interps.optionHandler1
      val s = SCtors1[SCtors1.Op]
      val program = List(s.x(1), s.x(2)).sequence[FreeS[SCtors1.Op, ?], Int]
      program.exec[Option] shouldBe (Some(List(1, 2)))
    }
  }
}
