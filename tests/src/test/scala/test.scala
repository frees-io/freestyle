package freestyle

import cats.free._
import org.scalatest.{Matchers, WordSpec}
import cats.implicits._
import cats.arrow.FunctionK
import cats.data.Coproduct

class tests extends WordSpec with Matchers {

  import algebras._

  "the @free annotation" should {

    "create a companion with a `Op` type alias" in {
      type Op[A] = SCtors1.Op[A]
    }

    "provide instances through it's companion `apply`" in {
      SCtors1[SCtors1.Op].isInstanceOf[SCtors1[SCtors1.Op]] shouldBe true
    }

    "allow implicit sumoning" in {
      implicitly[SCtors1[SCtors1.Op]].isInstanceOf[SCtors1[SCtors1.Op]] shouldBe true
    }

    "provide automatic implementations for smart constructors" in {
      val s = SCtors1[SCtors1.Op]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      program.isInstanceOf[FreeS[SCtors1.Op, Int]] shouldBe true
    }

    "respond to implicit evidences with compilable runtimes" in {
      implicit val optionHandler = interps.optionHandler1
      val s                          = SCtors1[SCtors1.Op]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      import freestyle.implicits._
      program.exec[Option] shouldBe Option(2)
    }

    "reuse program interpretation in diferent runtimes" in {
      implicit val optionHandler = interps.optionHandler1
      implicit val listHandler   = interps.listHandler1
      val s                          = SCtors1[SCtors1.Op]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      import freestyle.implicits._
      program.exec[Option] shouldBe Option(2)
      program.exec[List] shouldBe List(2)
    }

    "allow multiple args in smart constructors" in {
      @free
      trait MultiArgs[F[_]] {
        def x(a: Int, b: Int, c: Int): FreeS[F, Int]
      }
    }

    "allow smart constructors with no args" in {
      @free
      trait NoArgs[F[_]] {
        def x: FreeS[F, Int]
      }
    }

    "generate ADTs with friendly names and expose them as dependent types" in {
      @free
      trait FriendlyFreeS[F[_]] {
        def sc1(a: Int, b: Int, c: Int): FreeS[F, Int]
        def sc2(a: Int, b: Int, c: Int): FreeS[F, Int]
      }
      implicitly[FriendlyFreeS.Op[_] =:= FriendlyFreeS.Op[_]]
      implicitly[FriendlyFreeS.Sc1OP <:< FriendlyFreeS.Op[Int]]
      implicitly[FriendlyFreeS.Sc2OP <:< FriendlyFreeS.Op[Int]]
      ()
    }

    "allow smart constructors with type arguments" in {
      @free
      trait KVStore[F[_]] {
        def put[A](key: String, value: A): FreeS[F, Unit]
        def get[A](key: String): FreeS[F, Option[A]]
        def delete(key: String): FreeS[F, Unit]
      }
      val interpreter = new KVStore.Handler[List] {
        def put[A](key: String, value: A): List[Unit] = Nil
        def get[A](key: String): List[Option[A]]      = Nil
        def delete(key: String): List[Unit]           = Nil
      }
    }

    "allow evaluation of abstract members that return FreeS.Pars" in {
      @free
      trait ApplicativesServ[F[_]] {
        def x(key: String): FreeS.Par[F, String]
        def y(key: String): FreeS.Par[F, String]
        def z(key: String): FreeS.Par[F, String]
      }
      implicit val interpreter = new ApplicativesServ.Handler[Option] {
        override def x(key: String): Option[String] = Some(key)
        override def y(key: String): Option[String] = Some(key)
        override def z(key: String): Option[String] = Some(key)
      }
      val v = ApplicativesServ[ApplicativesServ.Op]
      import v._
      import freestyle.implicits._
      val program = (x("a") |@| y("b") |@| z("c")).map { _ + _ + _ }.freeS
      program.exec[Option] shouldBe Some("abc")
    }

    "allow sequential evaluation of combined FreeS & FreeS.Par" in {
      @free
      trait MixedFreeS[F[_]] {
        def x(key: String): FreeS.Par[F, String]
        def y(key: String): FreeS.Par[F, String]
        def z(key: String): FreeS[F, String]
      }
      implicit val interpreter = new MixedFreeS.Handler[Option] {
        override def x(key: String): Option[String] = Some(key)
        override def y(key: String): Option[String] = Some(key)
        override def z(key: String): Option[String] = Some(key)
      }
      val v = MixedFreeS[MixedFreeS.Op]
      import v._
      import freestyle.implicits._
      val apProgram = (x("a") |@| y("b")).map { _ + _ }
      val program = for {
        n <- z("1")
        m <- apProgram.freeS
      } yield n + m
      program.exec[Option] shouldBe Some("1ab")
    }

  }

  "the @module annotation" should {

    import modules._

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
      implicit val optionHandler1 = interps.optionHandler1
      implicit val optionHandler2 = interps.optionHandler2
      implicitly[FunctionK[M1.Op, Option]].isInstanceOf[FunctionK[M1.Op, Option]] shouldBe true
    }

    "[onion] find a FunctionK[Module.Op, ?] providing there is existing ones for it's smart constructors" in {
      import freestyle.implicits._
      implicit val optionHandler1 = interps.optionHandler1
      implicit val optionHandler2 = interps.optionHandler2
      implicit val optionHandler3 = interps.optionHandler3
      implicit val optionHandler4 = interps.optionHandler4
      implicitly[FunctionK[O1.Op, Option]].isInstanceOf[FunctionK[O1.Op, Option]] shouldBe true
    }

    "[simple] reuse program interpretation in diferent runtimes" in {
      import freestyle.implicits._
      implicit val optionHandler1 = interps.optionHandler1
      implicit val listHandler1   = interps.listHandler1
      implicit val optionHandler2 = interps.optionHandler2
      implicit val listHandler2   = interps.listHandler2
      val m1                          = M1[M1.Op]
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
      import freestyle.implicits._
      implicit val optionHandler1 = interps.optionHandler1
      implicit val listHandler1   = interps.listHandler1
      implicit val optionHandler2 = interps.optionHandler2
      implicit val listHandler2   = interps.listHandler2
      implicit val optionHandler3 = interps.optionHandler3
      implicit val listHandler3   = interps.listHandler3
      implicit val optionHandler4 = interps.optionHandler4
      implicit val listHandler4   = interps.listHandler4

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

  "Lifting syntax" should {

    "allow any value to be lifted into a FreeS monadic context" in {
      import freestyle.implicits._
      import cats.Eval
      import cats.implicits._

      def program[F[_]] =
        for {
          a <- Eval.now(1).freeS
          b <- 2.pure[Eval].freeS
          c <- 3.pure[Eval].freeS
        } yield a + b + c
      implicit val interpreter = FunctionK.id[Eval]
      program[Eval].exec[Eval].value shouldBe 6
    }

  }

  "Applicative Parallel Support" should {

    import algebras._

    class NonDeterminismTestShared {
      import freestyle.nondeterminism._
      import freestyle.implicits._

      val buf = scala.collection.mutable.ArrayBuffer.empty[Int]

      def blocker(value: Int, waitTime: Long): Int = {
        Thread.sleep(waitTime)
        buf += value
        value
      }

      val v = MixedFreeS[MixedFreeS.Op]
      import v._

      val program = for {
        a  <- z //3
        bc <- (x |@| y).tupled.freeS //(1,2)
        (b, c) = bc
        d <- z //3
      } yield a :: b :: c :: d :: Nil // List(3,1,2,3)

    }

    "allow non deterministic execution when interpreting to scala.concurrent.Future" in {
      import freestyle.nondeterminism._
      import freestyle.implicits._

      import scala.concurrent._
      import scala.concurrent.duration._
      import scala.concurrent.ExecutionContext.Implicits.global

      val test = new NonDeterminismTestShared
      import test._

      implicit val interpreter = new MixedFreeS.Handler[Future] {
        override def x: Future[Int] = Future(blocker(1, 1000L))
        override def y: Future[Int] = Future(blocker(2, 0L))
        override def z: Future[Int] = Future(blocker(3, 2000L))
      }

      Await.result(program.exec[Future], Duration.Inf) shouldBe List(3, 1, 2, 3)
      buf.toArray shouldBe Array(3, 2, 1, 3)
    }

    "allow non deterministic execution when interpreting to monix.eval.Task" in {
      import freestyle.nondeterminism._
      import freestyle.implicits._

      import scala.concurrent._
      import scala.concurrent.duration._
      import monix.cats._
      import monix.eval.Task
      import monix.eval.Task.nondeterminism
      import monix.execution.Scheduler.Implicits.global

      val test = new NonDeterminismTestShared
      import test._

      implicit val interpreter = new MixedFreeS.Handler[Task] {
        override def x: Task[Int] = Task(blocker(1, 1000L))
        override def y: Task[Int] = Task(blocker(2, 0L))
        override def z: Task[Int] = Task(blocker(3, 2000L))
      }

      Await.result(program.exec[Task].runAsync, Duration.Inf) shouldBe List(3, 1, 2, 3)
      buf.toArray shouldBe Array(3, 2, 1, 3)
    }

    "allow deterministic programs with FreeS.Par nodes run deterministically" in {
      import freestyle.nondeterminism._
      import freestyle.implicits._

      val test = new NonDeterminismTestShared
      import test._

      implicit val interpreter = new MixedFreeS.Handler[Option] {
        override def x: Option[Int] = Option(blocker(1, 1000L))
        override def y: Option[Int] = Option(blocker(2, 0L))
        override def z: Option[Int] = Option(blocker(3, 2000L))
      }

      program.exec[Option] shouldBe Option(List(3, 1, 2, 3))
      buf.toArray shouldBe Array(3, 1, 2, 3)
    }

    /**
     * Similar example as the one found at
     * http://typelevel.org/cats/datatypes/freeapplicative.html
     */
    "allow validation style algebras derived from FreeS.Par" in {
      import cats.data.Kleisli
      import cats.implicits._
      import scala.concurrent._
      import scala.concurrent.duration._
      import scala.concurrent.ExecutionContext.Implicits.global

      import freestyle.nondeterminism._
      import freestyle.implicits._

      type ParValidator[A] = Kleisli[Future, String, A]

      @free
      trait Validation[F[_]] {
        def minSize(n: Int): FreeS.Par[F, Boolean]
        def hasNumber: FreeS.Par[F, Boolean]
      }

      implicit val interpreter = new Validation.Handler[ParValidator] {
        override def minSize(n: Int): ParValidator[Boolean] =
          Kleisli(s => Future(s.size >= n))
        override def hasNumber: ParValidator[Boolean] =
          Kleisli(s => Future(s.exists(c => "0123456789".contains(c))))
      }

      val validation = Validation[Validation.Op]
      import validation._

      val parValidation = (minSize(3) |@| hasNumber).map(_ :: _ :: Nil)
      val validator     = parValidation.exec[ParValidator]

      Await.result(validator.run("a"), Duration.Inf) shouldBe List(false, false)
      Await.result(validator.run("abc"), Duration.Inf) shouldBe List(true, false)
      Await.result(validator.run("abc1"), Duration.Inf) shouldBe List(true, true)
      Await.result(validator.run("1a"), Duration.Inf) shouldBe List(false, true)
    }

    "generate code that works in the presentation compiler" in {
      import org.ensime.pcplod._
      withMrPlod("pcplodtest.scala") { pc =>
        pc.typeAtPoint('result) shouldBe Some("Option[Int]")
        pc.typeAtPoint('test) shouldBe Some("(n: Int)freestyle.FreeS[F,Int]")
        pc.typeAtPoint('handler) shouldBe Some("pcplodtest.PcplodTestAlgebra.Handler")
        pc.messages should be a 'empty
      }
    }

  }

}

object algebras {

  @free
  trait SCtors1[F[_]] {
    def x(a: Int): FreeS[F, Int]
    def y(a: Int): FreeS[F, Int]
  }

  @free
  trait SCtors2[F[_]] {
    def i(a: Int): FreeS[F, Int]
    def j(a: Int): FreeS[F, Int]
  }

  @free
  trait SCtors3[F[_]] {
    def o(a: Int): FreeS[F, Int]
    def p(a: Int): FreeS[F, Int]
  }

  @free
  trait SCtors4[F[_]] {
    def k(a: Int): FreeS[F, Int]
    def m(a: Int): FreeS[F, Int]
  }

  @free
  trait MixedFreeS[F[_]] {
    def x: FreeS.Par[F, Int]
    def y: FreeS.Par[F, Int]
    def z: FreeS[F, Int]
  }

  @free
  trait S1[F[_]] {
    def x(n: Int): FreeS[F, Int]
  }

  @free
  trait S2[F[_]] {
    def y(n: Int): FreeS[F, Int]
  }

}

object modules {

  import algebras._

  @module
  trait M1[F[_]] {
    val sctors1: SCtors1[F]
    val sctors2: SCtors2[F]
  }

  @module
  trait M2[F[_]] {
    val sctors3: SCtors3[F]
    val sctors4: SCtors4[F]
  }

  @module
  trait O1[F[_]] {
    val m1: M1[F]
    val m2: M2[F]
  }

  @module
  trait O2[F[_]] {
    val o1: O1[F]
    val x = 1
    def y = 2
  }

  @module
  trait O3[F[_]] {
    def x = 1
    def y = 2
  }

  @module
  trait StateProp[F[_]] {
    val s1: S1[F]
    val s2: S2[F]
  }

}

object interps {

  import algebras._

  val optionHandler1: FunctionK[SCtors1.Op, Option] = new SCtors1.Handler[Option] {
    def x(a: Int): Option[Int] = Some(a)
    def y(a: Int): Option[Int] = Some(a)
  }

  val listHandler1: FunctionK[SCtors1.Op, List] = new SCtors1.Handler[List] {
    def x(a: Int): List[Int] = List(a)
    def y(a: Int): List[Int] = List(a)
  }

  val optionHandler2: FunctionK[SCtors2.Op, Option] = new SCtors2.Handler[Option] {
    def i(a: Int): Option[Int] = Some(a)
    def j(a: Int): Option[Int] = Some(a)
  }

  val listHandler2: FunctionK[SCtors2.Op, List] = new SCtors2.Handler[List] {
    def i(a: Int): List[Int] = List(a)
    def j(a: Int): List[Int] = List(a)
  }

  val optionHandler3: FunctionK[SCtors3.Op, Option] = new SCtors3.Handler[Option] {
    def o(a: Int): Option[Int] = Some(a)
    def p(a: Int): Option[Int] = Some(a)
  }

  val listHandler3: FunctionK[SCtors3.Op, List] = new SCtors3.Handler[List] {
    def o(a: Int): List[Int] = List(a)
    def p(a: Int): List[Int] = List(a)
  }

  val optionHandler4: FunctionK[SCtors4.Op, Option] = new SCtors4.Handler[Option] {
    def k(a: Int): Option[Int] = Some(a)
    def m(a: Int): Option[Int] = Some(a)
  }

  val listHandler4: FunctionK[SCtors4.Op, List] = new SCtors4.Handler[List] {
    def k(a: Int): List[Int] = List(a)
    def m(a: Int): List[Int] = List(a)
  }
}
