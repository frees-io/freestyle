package freestyle

import cats.arrow.FunctionK
import cats.free._
import cats.implicits._
import org.scalatest.{Matchers, WordSpec}

class moduleTests extends WordSpec with Matchers {

  "the @module annotation" should {

    import algebras._
    import modules._
    import freestyle.implicits._
    import interps._

    "[simple] create a companion with a `T` type alias" in {
      type T[A] = M1.Op[A]
    }

    "[onion] create a companion with a `T` type alias" in {
      type T[A] = O1.Op[A]
    }

    "[simple] provide instances through it's companion `apply`" in {
      M1[M1.Op].isInstanceOf[M1[M1.Op]] shouldBe true
    }

    "[onion] provide instances through it's companion `apply`" in {
      O1[O1.Op].isInstanceOf[O1[O1.Op]] shouldBe true
    }

    "[simple] implicit sumoning" in {
      implicitly[M1[M1.Op]].isInstanceOf[M1[M1.Op]] shouldBe true
    }

    "[onion] allow implicit sumoning" in {
      implicitly[O1[O1.Op]].isInstanceOf[O1[O1.Op]] shouldBe true
    }

    "[simple] autowire implementations of it's contained smart constructors" in {
      val m1 = M1[M1.Op]
      m1.sctors1.isInstanceOf[SCtors1[M1.Op]] shouldBe true
      m1.sctors2.isInstanceOf[SCtors2[M1.Op]] shouldBe true
    }

    "[onion] autowire implementations of it's contained smart constructors" in {
      val o1 = O1[O1.Op]
      o1.m1.sctors1.isInstanceOf[SCtors1[O1.Op]] shouldBe true
      o1.m1.sctors2.isInstanceOf[SCtors2[O1.Op]] shouldBe true
      o1.m2.sctors3.isInstanceOf[SCtors3[O1.Op]] shouldBe true
      o1.m2.sctors4.isInstanceOf[SCtors4[O1.Op]] shouldBe true
    }

    "[simple] allow composition of it's contained algebras" in {
      val m1 = M1[M1.Op]
      val result = for {
        a <- m1.sctors1.x(1)
        b <- m1.sctors1.y(1)
        c <- m1.sctors2.i(1)
        d <- m1.sctors2.j(1)
      } yield a + b + c + d
      result.isInstanceOf[FreeS[M1.Op, Int]] shouldBe true
    }

    "[onion] allow composition of it's contained algebras" in {
      val o1 = O1[O1.Op]
      val result = for {
        a <- o1.m1.sctors1.x(1)
        b <- o1.m1.sctors1.y(1)
        c <- o1.m1.sctors2.i(1)
        d <- o1.m1.sctors2.j(1)
        e <- o1.m2.sctors3.o(1)
        f <- o1.m2.sctors3.p(1)
        g <- o1.m2.sctors4.k(1)
        h <- o1.m2.sctors4.m(1)
      } yield a + b + c + d + e + f + g + h
      result.isInstanceOf[FreeS[O1.Op, Int]] shouldBe true
    }

    "[simple] find a FunctionK[Module.Op, ?] providing there is existing ones for it's smart constructors" in {
      import freestyle.implicits._
      import interps.{optionHandler1, optionHandler2}
      implicitly[FunctionK[M1.Op, Option]].isInstanceOf[FunctionK[M1.Op, Option]] shouldBe true
    }

    "[onion] find a FunctionK[Module.Op, ?] providing there is existing ones for it's smart constructors" in {
      implicitly[FunctionK[O1.Op, Option]].isInstanceOf[FunctionK[O1.Op, Option]] shouldBe true
    }

    "[simple] reuse program interpretation in diferent runtimes" in {
      val m1 = M1[M1.Op]
      val program = for {
        a <- m1.sctors1.x(1)
        b <- m1.sctors1.y(1)
        c <- m1.sctors2.i(1)
        d <- m1.sctors2.j(1)
      } yield a + b + c + d
      program.exec[Option] shouldBe Option(4)
      program.exec[List] shouldBe List(4)
    }

    "[onion] reuse program interpretation in diferent runtimes" in {
      val o1 = O1[O1.Op]
      val program = for {
        a <- o1.m1.sctors1.x(1)
        b <- o1.m1.sctors1.y(1)
        c <- o1.m1.sctors2.i(1)
        d <- o1.m1.sctors2.j(1)
        e <- o1.m2.sctors3.o(1)
        f <- o1.m2.sctors3.p(1)
        g <- o1.m2.sctors4.k(1)
        h <- o1.m2.sctors4.m(1)
      } yield a + b + c + d + e + f + g + h

      program.exec[Option] shouldBe Option(8)
      program.exec[List] shouldBe List(8)
    }

    "Pass through concrete members to implementations" in {
      val o2 = O2[O2.Op]
      o2.x shouldBe 1
      o2.y shouldBe 2
    }

    "Allow modules with just concrete members unrelated to freestyle's concerns" in {
      val o3 = O3[O3.Op]
      o3.x shouldBe 1
      o3.y shouldBe 2
    }

  }

}
