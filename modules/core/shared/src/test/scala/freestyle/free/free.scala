/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle
package free

import cats.{Id, Monoid}
import cats.kernel.instances.int._
import cats.instances.option._
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import freestyle.free.implicits._
import org.scalatest.{Matchers, WordSpec}

class freeTests extends WordSpec with Matchers {

  /* Note that in many tests we can not use the "should compile" or "shouldNot compile", 
   *  due to the bag reported in https://github.com/scalatest/scalatest/issues/1150 */
  "the @free macro annotation should be rejected, and the compilation fail, if it is applied to" when {

    "an empty trait" in (
      "@free trait X" shouldNot compile
    )

    "an empty abstract class" in (
      "@free abstract class X" shouldNot compile
    )

    "a non-abstract class" in {
      "@free class X" shouldNot compile
    }

    "a trait with companion object" in {
      "@free trait X {def f: FS[Int]} ; object X" shouldNot compile
    }

    "an abstract class with a companion object" in {
      "@free trait X {def f: FS[Int]} ; object X" shouldNot compile
    }

    "a trait with any non-abstact methods of type FS" in {
      "@free trait X { def f(a: Char) : FS[Int] = 0 } " shouldNot compile
    }

  }

  "the @free macro annotation should be accepted if it is applied to" when {

    "a trait with at least one request" in {
      @free trait X { def bar(x:Int): FS[Int] }
    }

    "a trait with an F[_] bound type param" in {
      @free trait FBound[F[_]] { def bar(x:Int): FS[Int] }
    }

    "an abstract class with at least one request" in {
      @free abstract class X { def bar(x:Int): FS[Int] }
    }

    "a trait with an abstact method of type FS" in {
      @free trait X { def f(a: Char) : FS[Int] }
    }

    "a trait with type parameters" ignore {
      "@free trait X[A] { def ix(a: A) : FS[A] }" should compile
    }

    "a trait with some concrete non-FS members" in {
      @free trait X {
        def x: FS[Int]
        def y: Int = 5
        val z: Int = 6
      }
      implicit object IdX extends X.Handler[Id]{
        def x: Int = 31
      }
      0 shouldEqual 0
    }

  }

  "the @free macro should preserve the shape of the parameters of the request" when {

    "there are no parameters" in {
      @free trait X { def f: FS[Int] }
      object Y extends X.Handler[Id] { def f: Int = 42 }
      Y.f shouldEqual 42
    }

    "there is one list with multiple params" in {
      @free trait X {
        def f(a: Int, b: Int): FS[Int]
      }
      object Y extends X.Handler[Id] {
        override def f(a: Int, b: Int): Int = 42
      }
      Y.f(2,3) shouldEqual 42
    }

    "there are multiple lists of parameters" in {
      @free trait X {
        def f(a: Int)(b: Int): FS[Int]
      }
      object Y extends X.Handler[Id] {
        override def f(a: Int)(b: Int): Int = 42
      }
      Y.f(2)(3) shouldEqual 42
    }

    "there are multiple lists of parameters, with the last one being implicit" in {
      @free trait X {
        def f(a: Int)(implicit b: Int): FS[Int]
      }
      object Y extends X.Handler[Id] {
        override def f(a: Int)(implicit b: Int): Int = 42
      }
      implicit val x: Int = 3
      val res: Int = Y.f(2)
      res shouldEqual 42
    }

    "there is one type parameter with a type-class bound, and no arguments"  in {
      @free trait X {
        def g[T: Monoid]: FS[T]
      }
      object Y extends X.Handler[Id]{
        def g[T]()(implicit x: Monoid[T]): T = x.empty
      }
      Y.g[Int] shouldEqual 0
    }

    "there is one type parameter with a type-class bound, with arguments"  in {
      @free trait X {
        def f[T: Monoid](a: T): FS[T]
      }
      object Y extends X.Handler[Id]{
        def f[T](a: T)(implicit x: Monoid[T]): T = a
      }
      Y.f[Int](42) shouldEqual 42
    }

  }

  "the @free macro should handle type parameters in the request" when {

    "there is one type parameter in the method" in {
      @free trait X {
        def ix[A](a: A): FS[A]
      }
      object Y extends X.Handler[Id] {
        def ix[A](a: A): A = a
      }
      Y.ix[Int](5) shouldEqual 5
    }

    "there are one upper-bounds or one lower bound in the method" in {
      @free trait X {
        def f[A <: Int](a: A) : FS[A]
        def g[A >: Int](a: A) : FS[A]
      }
      object Y extends X.Handler[Id] {
        def f[A <: Int](a: A): A = a
        def g[A >: Int](a: A): A = a
      }
      Y.f[Int](5) shouldEqual 5
      Y.g[Int](5) shouldEqual 5
    }

    "a trait with different type parameters in the method" in {
      @free trait X {
        def ix[A <: Int, B, C >: Int](a: A, b: B, c: C) : FS[A]
      }
    }

    "a trait with high bounded type parameters in the method" in {
      """
        trait X[A]
        @free trait Y { def ix[A <: Int : X](a: A) : FS[A] }
      """ should compile
    }

  }

  "a @free trait can define methods of type FS.Par and FS.Seq by combining FS through" when {

    "the use of the `map` operation from Functor, to derive a  FS.Par" in {
      @free trait X {
        def a: FS[Int]
        def b: FS.Par[Int] = a.map(x => x+1)
      }
      implicit object IdX extends X.Handler[Id] {
        def a: Int = 41
      }
      X.to[X.Op].b.interpret[Id] shouldEqual 42
    }

    "using the Applicative instance to combine operations into a FS.Par" in {
      @free trait X {
        def a: FS[Int]
        def b: FS.Par[Int] = (a, a).mapN(_+_)
      }
      implicit object Y extends X.Handler[Id] {
        def a: Int = 5
      }
      X.to[X.Op].b.interpret[Id] shouldEqual 10
    }

    "using the Monad instance to combine operations into a FS.Seq" in {
      @free trait X {
        def a: FS[Int]
        def b(x: Int): FS[Int]
        def c: FS.Seq[Int] = {
          val fa: FS.Seq[Int] = a.freeS
          fa.flatMap(x => b(x).freeS)
        }
      }
      implicit object IdX extends X.Handler[Id] {
        def a: Int = 41
        def b(x: Int) = x+1
      }
      X.to[X.Op].c.interpret[Id] shouldEqual 42
    }

    "mixing all of the above" in {
      @free trait X {
        def a: FS[Int]
        def b(i: Int): FS.Par[Int] = a.map(x => x+i)
        def c: FS.Par[Int] = (a,a).mapN(_+_)
        def d: FS.Seq[Int] = c.freeS.flatMap(x => b(x).freeS)
      }

    }
  }

  "the @free annotation" should {

    "create a companion with a `Op` type alias" in {
      "type Op[A] = SCtors1.Op[A]" should compile
    }

    "provide instances through it's companion `apply`" in {
      "SCtors1[SCtors1.Op]" should compile
    }

    "allow implicit summoning" in {
      "implicitly[SCtors1[SCtors1.Op]]" should compile
    }

    "provide automatic implementations for smart constructors" in {
      val s = SCtors1[SCtors1.Op]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      (program: FreeS[SCtors1.Op, Int])
    }

    "generate ADTs with friendly names and expose them as dependent types" in {
      @free
      trait FriendlyFreeS {
        def sc1(a: Int, b: Int, c: Int): FS[Int]
        def sc2(a: Int, b: Int, c: Int): FS[Int]
      }
      implicitly[FriendlyFreeS.Op[_] =:= FriendlyFreeS.Op[_]]
      implicitly[FriendlyFreeS.Sc1Op <:< FriendlyFreeS.Op[Int]]
      implicitly[FriendlyFreeS.Sc2Op <:< FriendlyFreeS.Op[Int]]
      1 shouldEqual 1
    }

  }

  "the @free macro annotation works together with @debug annotation" when {

    "a trait without @free macro annotation is ignored" ignore {
      "@debug trait X { def f(a: Char) : FS[Int] }" should compile
    }

    "a trait with at least one request" in {
      "@free @debug trait Y { def bar(x:Int): FS[Int] }" should compile
    }

    "an abstract class with at least one request" in {
      "@debug @free abstract class Z { def bar(x:Int): FS[Int] }" should compile
    }

  }

  "The @free annotation should generate interpreters or Handlers that" should {

    "allow `FreeS` operations that use other abstract operations" in {
      @free trait Combine {
        def x(a: Int): FS[Int]
        def y(a: Int): FS[Boolean] = x(a).map { _ >= 0 }
      }
      val v = Combine[Combine.Op]
      implicit val interpreter: Combine.Handler[Id] =
        new Combine.Handler[Id] {
          override def x(a: Int): Int = 4
        }
      v.y(5).interpret[Id] shouldBe (true)
    }

    "let implicits args reach handlers as explicit args" in {
      type X = String
      implicit val x: X = "x"
      @free trait AlgWithImplicits {
        def x(a: Int)(implicit ev: X): FS[X]
      }
      implicit val h: AlgWithImplicits.Handler[Id] =
        new AlgWithImplicits.Handler[Id] {
          def x(a: Int)(implicit ev: X): Id[X] = ev.toString + a.toString
        }
      AlgWithImplicits[AlgWithImplicits.Op].x(1).interpret[Id] shouldBe ("x1")
    }

    "let context bound implicits reach handlers as explicit args" in {
      trait X[A] {
        def z: String
      }
      @free trait AlgWithImplicits {
        def x[A: X](a: A): FS[X[A]]
      }
      implicit def xa[A]: X[A] = new X[A] {
        def z = "a"
      }
      implicit val h: AlgWithImplicits.Handler[Id] = new AlgWithImplicits.Handler[Id] {
        def x[A](a: A)(implicit ev: X[A]): Id[X[A]] = ev
      }
      AlgWithImplicits[AlgWithImplicits.Op].x(1).interpret[Id].z shouldBe (xa.z)
    }

    "let multiple context bound implicits reach handlers as explicit args" in {
      trait X[A] {
        def z: String
      }
      trait Y[A] {
        def z: String
      }
      @free trait AlgWithImplicits {
        def x[A: X: Y](a: A): FS[(X[A], Y[A])]
      }
      implicit def xa[A]: X[A] = new X[A] {
        def z = "xa"
      }
      implicit def ya[A]: Y[A] = new Y[A] {
        def z = "ya"
      }
      implicit val h: AlgWithImplicits.Handler[Id] = new AlgWithImplicits.Handler[Id] {
        def x[A](a: A)(implicit x: X[A], y: Y[A]): Id[(X[A], Y[A])] = (x, y)
      }
      val (x, y) = AlgWithImplicits[AlgWithImplicits.Op].x(1).interpret[Id]
      (x.z, y.z) shouldBe (("xa", "ya"))
    }

    "let mixed args and context bounds implicits reach handlers as explicit args" in {
      trait X[A] {
        def y: String = "x"
      }
      type S = String
      @free trait AlgWithImplicits {
        def x[A: X](a: A)(implicit s: S): FS[String]
      }
      implicit val s: S        = "s"
      implicit def xa[A]: X[A] = new X[A] {}
      implicit val h: AlgWithImplicits.Handler[Id] = new AlgWithImplicits.Handler[Id] {
        def x[A](a: A)(implicit s: S, ev: X[A]): Id[String] = ev.y + s
      }
      AlgWithImplicits[AlgWithImplicits.Op].x(1).interpret[Id] shouldBe "xs"
    }

    "respond to implicit evidences with compilable runtimes" in {
      implicit val optionHandler: FSHandler[SCtors1.Op, Option] = interps.optionHandler1

      val s = SCtors1[SCtors1.Op]

      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b

      program.interpret[Option] shouldBe Option(2)
    }

    "reuse program interpretation in diferent runtimes" in {
      implicit val optionHandler: FSHandler[SCtors1.Op, Option] = interps.optionHandler1
      implicit val listHandler: FSHandler[SCtors1.Op, List]     = interps.listHandler1

      val s = SCtors1[SCtors1.Op]

      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b

      program.interpret[Option] shouldBe Option(2)
      program.interpret[List] shouldBe List(2)
    }

    "allow evaluation of abstract members that return FreeS.Pars" in {
      @free
      trait ApplicativesServ {
        def x(key: String): FS[String]
        def y(key: String): FS[String]
        def z(key: String): FS[String]
      }
      implicit val interpreter: ApplicativesServ.Handler[Option] =
        new ApplicativesServ.Handler[Option] {
          override def x(key: String): Option[String] = Some(key)
          override def y(key: String): Option[String] = Some(key)
          override def z(key: String): Option[String] = Some(key)
        }
      val v = ApplicativesServ[ApplicativesServ.Op]
      import v._
      val program = (x("a"), y("b"), z("c")).mapN { _ + _ + _ }.freeS
      program.interpret[Option] shouldBe Some("abc")
    }

    "allow sequential evaluation of combined FreeS & FreeS.Par" in {
      @free
      trait MixedFreeS {
        def x(key: String): FS[String]
        def y(key: String): FS[String]
        def z(key: String): FS[String]
      }
      implicit val interpreter: MixedFreeS.Handler[Option] =
        new MixedFreeS.Handler[Option] {
          override def x(key: String): Option[String] = Some(key)
          override def y(key: String): Option[String] = Some(key)
          override def z(key: String): Option[String] = Some(key)
        }
      val v = MixedFreeS[MixedFreeS.Op]
      import v._
      val apProgram = (x("a"), y("b")).mapN { _ + _ }
      val program = for {
        n <- z("1")
        m <- apProgram.freeS
      } yield n + m
      program.interpret[Option] shouldBe Some("1ab")
    }

  }

  "@free annotation, other things:" should {

    "allow non-FreeS concrete definitions in the trait" in {
      @free trait WithExtra {
        def x(a: Int): FS[String]
        def y: Int = 5
        val z: Int = 6
      }
      val v = WithExtra.instance
      v.y shouldBe 5
      v.z shouldBe 6

      implicit val interpreter: WithExtra.Handler[Id] =
        new WithExtra.Handler[Id] {
          override def x(a: Int): String = a.toString
        }
      v.x(v.z).interpret[Id] shouldBe "6"
    }

    "allow `FreeS` operations that use other abstractoperations" in {
      @free trait Combine {
        def x(a: Int): FS[Int]
        def y(a: Int): FS[Boolean] = x(a).map { _ >= 0 }
      }
      val v = Combine[Combine.Op]
      implicit val interpreter: Combine.Handler[Id] =
        new Combine.Handler[Id] {
          override def x(a: Int): Int = 4
        }
      v.y(5).interpret[Id] shouldBe (true)
    }

    "generate interpreters or Handlers when mix higher bound params and implicits" in {
      trait X[A]
      @free trait Algebra {
        def x[A <: String: X](a: A): FS[Int]
      }
      val v = Algebra[Algebra.Op]
      implicit val interpreter: Algebra.Handler[Id] =
        new Algebra.Handler[Id] {
          override def x[A <: String](a: A)(implicit x: X[A]): Int = 4
        }
      implicit def x[A]: X[A] = new X[A] {}
      v.x("").interpret[Id] shouldBe 4
    }

  }

}
