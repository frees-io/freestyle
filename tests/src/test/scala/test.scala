package io.freestyle

import cats.free._
import org.scalatest.{WordSpec, Matchers}
import cats.implicits._
import cats.arrow.FunctionK
import cats.data.Coproduct

class tests extends WordSpec with Matchers {

  import algebras._

  "the @free annotation" should {

    "create a companion with a `T` type alias" in {
      type T[A] = SCtors1.T[A]
    }

    "provide instances through it's companion `apply`" in {
      SCtors1[SCtors1.T].isInstanceOf[SCtors1[SCtors1.T]] shouldBe true
    }

    "allow implicit sumoning" in {
      implicitly[SCtors1[SCtors1.T]].isInstanceOf[SCtors1[SCtors1.T]] shouldBe true
    }

    "provide automatic implementations for smart constructors" in {
      val s = SCtors1[SCtors1.T]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      program.isInstanceOf[FreeS[SCtors1.T, Int]] shouldBe true
    }

    "respond to implicit evidences with compilable runtimes" in {
      implicit val optionInterpreter = interpreters.optionInterpreter1
      val s = SCtors1[SCtors1.T]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      import io.freestyle.implicits._
      program.exec[Option] shouldBe Option(2)
    }

    "reuse program interpretation in diferent runtimes" in {
      implicit val optionInterpreter = interpreters.optionInterpreter1
      implicit val listInterpreter = interpreters.listInterpreter1
      val s = SCtors1[SCtors1.T]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      import io.freestyle.implicits._
      program.exec[Option] shouldBe Option(2)
      program.exec[List] shouldBe List(2)
    }

    "allow multiple args in smart constructors" in {
      @free trait MultiArgs[F[_]] {
        def x(a: Int, b: Int, c: Int): FreeS[F, Int]
      }
    }

    "allow smart constructors with no args" in {
      @free trait NoArgs[F[_]] {
        def x: FreeS[F, Int]
      }
    }

    "generate ADTs with friendly names and expose them as dependent types" in {
      @free trait FriendlyFreeS[F[_]] {
        def sc1(a: Int, b: Int, c: Int): FreeS[F, Int]
        def sc2(a: Int, b: Int, c: Int): FreeS[F, Int]
      }
      implicitly[FriendlyFreeS.T[_] =:= FriendlyFreeS.FriendlyFreeSOP[_]]
      implicitly[FriendlyFreeS.Sc1OP <:< FriendlyFreeS.T[Int]]
      implicitly[FriendlyFreeS.Sc2OP <:< FriendlyFreeS.T[Int]]
      ()
    }

    "allow smart constructors with type arguments" in {
      @free trait KVStore[F[_]] {
        def put[A](key: String, value: A): FreeS[F, Unit]
        def get[A](key: String): FreeS[F, Option[A]]
        def delete(key: String): FreeS[F, Unit]
      }
      val interpreter = new KVStore.Interpreter[List] {
        def putImpl[A](key: String, value: A): List[Unit] = Nil
        def getImpl[A](key: String): List[Option[A]] = Nil
        def deleteImpl(key: String): List[Unit] = Nil
      }
    }

    "allow evaluation of abstract members that return FreeS.Pars" in {
      @free trait ApplicativesServ[F[_]] {
        def x(key: String): FreeS.Par[F, String]
        def y(key: String): FreeS.Par[F, String]
        def z(key: String): FreeS.Par[F, String]
      }
      implicit val interpreter = new ApplicativesServ.Interpreter[Option] {
        override def xImpl(key: String): Option[String] = Some(key)
        override def yImpl(key: String): Option[String] = Some(key)
        override def zImpl(key: String): Option[String] = Some(key)
      }
      val v = ApplicativesServ[ApplicativesServ.T]
      import v._
      import io.freestyle.implicits._
      val program = (x("a") |@| y("b") |@| z("c")).map {_ + _ + _}.seq
      program.exec[Option] shouldBe Some("abc")
    }

    "allow sequential evaluation of combined Free & FreeS.Pars" in {
      @free trait MixedFreeS[F[_]] {
        def x(key: String): FreeS.Par[F, String]
        def y(key: String): FreeS.Par[F, String]
        def z(key: String): FreeS[F, String]
      }
      implicit val interpreter = new MixedFreeS.Interpreter[Option] {
        override def xImpl(key: String): Option[String] = Some(key)
        override def yImpl(key: String): Option[String] = Some(key)
        override def zImpl(key: String): Option[String] = Some(key)
      }
      val v = MixedFreeS[MixedFreeS.T]
      import v._
      import io.freestyle.implicits._
      val apProgram = (x("a") |@| y("b")).map {_ + _}
      val program = for {
        n <- z("1")
        m <- apProgram.seq
      } yield n + m
      program.exec[Option] shouldBe Some("1ab")
    }

  }

  "the @module annotation" should {

    import modules._

    "[simple] create a companion with a `T` type alias" in {
      type T[A] = M1.T[A]
    }

    "[onion] create a companion with a `T` type alias" in {
      type T[A] = O1.T[A]
    }

    "[simple] provide instances through it's companion `apply`" in {
      M1[M1.T].isInstanceOf[M1[M1.T]] shouldBe true
    }

    "[onion] provide instances through it's companion `apply`" in {
      O1[O1.T].isInstanceOf[O1[O1.T]] shouldBe true
    }

    "[simple] implicit sumoning" in {
      implicitly[M1[M1.T]].isInstanceOf[M1[M1.T]] shouldBe true
    }

    "[onion] allow implicit sumoning" in {
      implicitly[O1[O1.T]].isInstanceOf[O1[O1.T]] shouldBe true
    }

    "[simple] autowire implementations of it's contained smart constructors" in {
      val m1 = M1[M1.T]
      m1.sctors1.isInstanceOf[SCtors1[M1.T]] shouldBe true
      m1.sctors2.isInstanceOf[SCtors2[M1.T]] shouldBe true
    }

    "[onion] autowire implementations of it's contained smart constructors" in {
      val o1 = O1[O1.T]
      o1.m1.sctors1.isInstanceOf[SCtors1[O1.T]] shouldBe true
      o1.m1.sctors2.isInstanceOf[SCtors2[O1.T]] shouldBe true
      o1.m2.sctors3.isInstanceOf[SCtors3[O1.T]] shouldBe true
      o1.m2.sctors4.isInstanceOf[SCtors4[O1.T]] shouldBe true
    }

    "[simple] allow composition of it's contained algebras" in {
      val m1 = M1[M1.T]
      val result = for {
        a <- m1.sctors1.x(1)
        b <- m1.sctors1.y(1)
        c <- m1.sctors2.i(1)
        d <- m1.sctors2.j(1)
      } yield a + b + c + d
      result.isInstanceOf[FreeS[M1.T, Int]] shouldBe true
    }

    "[onion] allow composition of it's contained algebras" in {
      val o1 = O1[O1.T]
      val result = for {
        a <- o1.m1.sctors1.x(1)
        b <- o1.m1.sctors1.y(1)
        c <- o1.m1.sctors2.i(1)
        d <- o1.m1.sctors2.j(1)
        e <- o1.m2.sctors3.o(1)
        f <- o1.m2.sctors3.p(1)
        g <- o1.m2.sctors4.k(1)
        h <- o1.m2.sctors4.l(1)
      } yield a + b + c + d + e + f + g + h
      result.isInstanceOf[FreeS[O1.T, Int]] shouldBe true
    }

    "[simple] find a FunctionK[Module.T, ?] providing there is existing ones for it's smart constructors" in {
      import io.freestyle.implicits._
      implicit val optionInterpreter1 = interpreters.optionInterpreter1
      implicit val optionInterpreter2 = interpreters.optionInterpreter2
      implicitly[FunctionK[M1.T, Option]].isInstanceOf[FunctionK[M1.T, Option]] shouldBe true
    }

    "[onion] find a FunctionK[Module.T, ?] providing there is existing ones for it's smart constructors" in {
      import io.freestyle.implicits._
      implicit val optionInterpreter1 = interpreters.optionInterpreter1
      implicit val optionInterpreter2 = interpreters.optionInterpreter2
      implicit val optionInterpreter3 = interpreters.optionInterpreter3
      implicit val optionInterpreter4 = interpreters.optionInterpreter4
      implicitly[FunctionK[O1.T, Option]].isInstanceOf[FunctionK[O1.T, Option]] shouldBe true
    }

    "[simple] reuse program interpretation in diferent runtimes" in {
      import io.freestyle.implicits._
      implicit val optionInterpreter1 = interpreters.optionInterpreter1
      implicit val listInterpreter1 = interpreters.listInterpreter1
      implicit val optionInterpreter2 = interpreters.optionInterpreter2
      implicit val listInterpreter2 = interpreters.listInterpreter2
      val m1 = M1[M1.T]
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
      import io.freestyle.implicits._
      implicit val optionInterpreter1 = interpreters.optionInterpreter1
      implicit val listInterpreter1 = interpreters.listInterpreter1
      implicit val optionInterpreter2 = interpreters.optionInterpreter2
      implicit val listInterpreter2 = interpreters.listInterpreter2
      implicit val optionInterpreter3 = interpreters.optionInterpreter3
      implicit val listInterpreter3 = interpreters.listInterpreter3
      implicit val optionInterpreter4 = interpreters.optionInterpreter4
      implicit val listInterpreter4 = interpreters.listInterpreter4

      val o1 = O1[O1.T]
      val program = for {
        a <- o1.m1.sctors1.x(1)
        b <- o1.m1.sctors1.y(1)
        c <- o1.m1.sctors2.i(1)
        d <- o1.m1.sctors2.j(1)
        e <- o1.m2.sctors3.o(1)
        f <- o1.m2.sctors3.p(1)
        g <- o1.m2.sctors4.k(1)
        h <- o1.m2.sctors4.l(1)
      } yield a + b + c + d + e + f + g + h

      program.exec[Option] shouldBe Option(8)
      program.exec[List] shouldBe List(8)
    }

    "Pass through concrete members to implementations" in {
      val o2 = O2[O2.T]
      o2.x shouldBe 1
      o2.y shouldBe 2
    }

    "Allow modules with just concrete members unrelated to freestyle's concerns" in {
      val o3 = O3[O3.T]
      o3.x shouldBe 1
      o3.y shouldBe 2
    }

  }

  "Applicative Parallel Support" should {

    import modules._

    "allow non deterministic programs for unsafe Non-deterministic targets" in {
      import scala.concurrent._
      import scala.concurrent.duration._
      import scala.concurrent.ExecutionContext.Implicits.global

      @free trait MixedFreeS[F[_]] {
        def x: FreeS.Par[F, Int]
        def y: FreeS.Par[F, Int]
        def z: FreeS[F, Int]
      }

      val buf = scala.collection.mutable.ArrayBuffer.empty[Int]

      def blocker(value: Int, waitTime: Long): Int = {
        Thread.sleep(waitTime)
        buf += value
        value
      }

      implicit val interpreter = new MixedFreeS.Interpreter[Future] {
        override def xImpl: Future[Int] = Future(blocker(1, 1000L))
        override def yImpl: Future[Int] = Future(blocker(2, 0L))
        override def zImpl: Future[Int] = Future(blocker(3, 2000L))
      }

      val v = MixedFreeS[MixedFreeS.T]
      import v._
      import io.freestyle.implicits._
      import io.freestyle.implicits.parallel._
      val program = for {
        a <- z //3
        bc <- (x |@| y).tupled.seq //(1,2)
        (b, c) = bc
        d <- z //3
      } yield a :: b :: c :: d :: Nil // List(3,1,2,3)
      Await.result(program.exec[Future], Duration.Inf) shouldBe List(3,1,2,3)
      buf.toArray shouldBe Array(3,2,1,3)
    }

  }

}

object algebras {

  @free trait SCtors1[F[_]] {
    def x(a: Int): FreeS[F, Int]
    def y(a: Int): FreeS[F, Int]
  }

  @free trait SCtors2[F[_]] {
    def i(a: Int): FreeS[F, Int]
    def j(a: Int): FreeS[F, Int]
  }

  @free trait SCtors3[F[_]] {
    def o(a: Int): FreeS[F, Int]
    def p(a: Int): FreeS[F, Int]
  }

  @free trait SCtors4[F[_]] {
    def k(a: Int): FreeS[F, Int]
    def l(a: Int): FreeS[F, Int]
  }

}

object modules {

  import algebras._

  @module trait M1[F[_]] {
    val sctors1: SCtors1[F]
    val sctors2: SCtors2[F]
  }

  @module trait M2[F[_]] {
    val sctors3: SCtors3[F]
    val sctors4: SCtors4[F]
  }

  @module trait O1[F[_]] {
    val m1: M1[F]
    val m2: M2[F]
  }

  @module trait O2[F[_]] {
    val o1: O1[F]
    val x = 1
    def y = 2
  }

  @module trait O3[F[_]] {
    def x = 1
    def y = 2
  }

}

object interpreters {

  import algebras._

  val optionInterpreter1: FunctionK[SCtors1.T, Option] = new SCtors1.Interpreter[Option] {
    def xImpl(a: Int): Option[Int] = Some(a)
    def yImpl(a: Int): Option[Int] = Some(a)
  }

  val listInterpreter1: FunctionK[SCtors1.T, List] = new SCtors1.Interpreter[List] {
    def xImpl(a: Int): List[Int] = List(a)
    def yImpl(a: Int): List[Int] = List(a)
  }

  val optionInterpreter2: FunctionK[SCtors2.T, Option] = new SCtors2.Interpreter[Option] {
    def iImpl(a: Int): Option[Int] = Some(a)
    def jImpl(a: Int): Option[Int] = Some(a)
  }

  val listInterpreter2: FunctionK[SCtors2.T, List] = new SCtors2.Interpreter[List] {
    def iImpl(a: Int): List[Int] = List(a)
    def jImpl(a: Int): List[Int] = List(a)
  }

  val optionInterpreter3: FunctionK[SCtors3.T, Option] = new SCtors3.Interpreter[Option] {
    def oImpl(a: Int): Option[Int] = Some(a)
    def pImpl(a: Int): Option[Int] = Some(a)
  }

  val listInterpreter3: FunctionK[SCtors3.T, List] = new SCtors3.Interpreter[List] {
    def oImpl(a: Int): List[Int] = List(a)
    def pImpl(a: Int): List[Int] = List(a)
  }

  val optionInterpreter4: FunctionK[SCtors4.T, Option] = new SCtors4.Interpreter[Option] {
    def kImpl(a: Int): Option[Int] = Some(a)
    def lImpl(a: Int): Option[Int] = Some(a)
  }

  val listInterpreter4: FunctionK[SCtors4.T, List] = new SCtors4.Interpreter[List] {
    def kImpl(a: Int): List[Int] = List(a)
    def lImpl(a: Int): List[Int] = List(a)
  }
}
