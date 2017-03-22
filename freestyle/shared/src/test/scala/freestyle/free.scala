package freestyle

import cats.implicits._
import cats.Id
import org.scalatest.{Matchers, WordSpec}

class freeTests extends WordSpec with Matchers {

  "the @free annotation" should {

    import algebras._
    import freestyle.implicits._

    "create a companion with a `Op` type alias" in {
      type Op[A] = SCtors1.Op[A]
    }

    "provide instances through it's companion `apply`" in {
      SCtors1.to[SCtors1.Op].isInstanceOf[SCtors1.To[SCtors1.Op]] shouldBe true
    }

    "allow implicit sumoning" in {
      implicitly[SCtors1.To[SCtors1.Op]].isInstanceOf[SCtors1.To[SCtors1.Op]] shouldBe true
    }

    "provide automatic implementations for smart constructors" in {
      val s = SCtors1.to[SCtors1.Op]
      import s._
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      program.isInstanceOf[FreeS[SCtors1.Op, Int]] shouldBe true
    }

    "respond to implicit evidences with compilable runtimes" in {
      implicit val optionHandler = interps.optionHandler1
      val s = SCtors1.to[SCtors1.Op]
      import s._
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      program.exec[Option] shouldBe Option(2)
    }

    "reuse program interpretation in diferent runtimes" in {
      implicit val optionHandler = interps.optionHandler1
      implicit val listHandler   = interps.listHandler1
      val s = SCtors1.to[SCtors1.Op]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      program.exec[Option] shouldBe Option(2)
      program.exec[List] shouldBe List(2)
    }

    "allow multiple args in smart constructors" in {
      @free
      trait MultiArgs {
        def x(a: Int, b: Int, c: Int): OpSeq[Int]
      }
    }

    "allow smart constructors with no args" in {
      @free
      trait NoArgs {
        def x: OpSeq[Int]
      }
    }

    "generate ADTs with friendly names and expose them as dependent types" in {
      @free
      trait FriendlyFreeS {
        def sc1(a: Int, b: Int, c: Int): OpSeq[Int]
        def sc2(a: Int, b: Int, c: Int): OpSeq[Int]
      }
      implicitly[FriendlyFreeS.Op[_] =:= FriendlyFreeS.Op[_]]
      implicitly[FriendlyFreeS.Sc1OP <:< FriendlyFreeS.Op[Int]]
      implicitly[FriendlyFreeS.Sc2OP <:< FriendlyFreeS.Op[Int]]
      ()
    }

    "allow smart constructors with type arguments" in {
      @free
      trait KVStore {
        def put[A](key: String, value: A): OpSeq[Unit]
        def get[A](key: String): OpSeq[Option[A]]
        def delete(key: String): OpSeq[Unit]
      }
      val interpreter = new KVStore.Handler[List] {
        def put[A](key: String, value: A): List[Unit] = Nil
        def get[A](key: String): List[Option[A]]      = Nil
        def delete(key: String): List[Unit]           = Nil
      }
    }

    "allow evaluation of abstract members that return FreeS.Pars" in {
      @free
      trait ApplicativesServ {
        def x(key: String): OpPar[String]
        def y(key: String): OpPar[String]
        def z(key: String): OpPar[String]
      }
      implicit val interpreter = new ApplicativesServ.Handler[Option] {
        override def x(key: String): Option[String] = Some(key)
        override def y(key: String): Option[String] = Some(key)
        override def z(key: String): Option[String] = Some(key)
      }
      val v = ApplicativesServ.to[ApplicativesServ.Op]
      import v._
      val program = (x("a") |@| y("b") |@| z("c")).map { _ + _ + _ }.freeS
      program.exec[Option] shouldBe Some("abc")
    }

    "allow sequential evaluation of combined FreeS & FreeS.Par" in {
      @free
      trait MixedFreeS {
        def x(key: String): OpPar[String]
        def y(key: String): OpPar[String]
        def z(key: String): OpSeq[String]
      }
      implicit val interpreter = new MixedFreeS.Handler[Option] {
        override def x(key: String): Option[String] = Some(key)
        override def y(key: String): Option[String] = Some(key)
        override def z(key: String): Option[String] = Some(key)
      }
      val v = MixedFreeS.to[MixedFreeS.Op]
      import v._
      val apProgram = (x("a") |@| y("b")).map { _ + _ }
      val program = for {
        n <- z("1")
        m <- apProgram.freeS
      } yield n + m
      program.exec[Option] shouldBe Some("1ab")
    }

    "allow non-FreeS concrete definitions in the trait" in {
      @free trait WithExtra {
        def x(a: Int): OpPar[String]
        def y: Int = 5
        val z: Int = 6
      }
      val v = WithExtra.to[WithExtra.Op]
      v.y shouldBe 5
      v.z shouldBe 6

      implicit val interpreter = new WithExtra.Handler[Id]{
        override def x(a: Int): String = a.toString
      }
      v.x(v.z).exec[Id] shouldBe "6"
    }

    "allow `FreeS` operations that use other abstractoperations" in {
      @free trait Combine {
        def x(a: Int): OpSeq[Int]
        def y(a: Int): OpSeq[Boolean] = x(a).map { _ >= 0 }
      }
      val v = Combine.to[Combine.Op]
      implicit val interpreter = new Combine.Handler[Id]{
        override def x(a: Int): Int = 4
      }
      v.y(5).exec[Id] shouldBe(true)
    }

  }

}
