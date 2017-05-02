/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

import cats.implicits._
import cats.Id
import org.scalatest.{Matchers, WordSpec}

class freeTests extends WordSpec with Matchers {

  "the @free annotation" should {

    import freestyle.implicits._

    "be rejected if applied to a non-abstract class" in {
      """@free class Foo { val x: Int}""" shouldNot compile
    }

    "be rejected if applied to a trait with companion object" in {
      """ @free trait Foo {def f: FS[Int]} ; object Foo """ shouldNot compile
    }

    "create a companion with a `Op` type alias" in {
      type Op[A] = SCtors1.Op[A]
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
      "(program: FreeS[SCtors1.Op, Int])" should compile
    }

    "respond to implicit evidences with compilable runtimes" in {
      implicit val optionHandler = interps.optionHandler1
      val s                      = SCtors1[SCtors1.Op]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      program.interpret[Option] shouldBe Option(2)
    }

    "reuse program interpretation in diferent runtimes" in {
      implicit val optionHandler = interps.optionHandler1
      implicit val listHandler   = interps.listHandler1
      val s                      = SCtors1[SCtors1.Op]
      val program = for {
        a <- s.x(1)
        b <- s.y(1)
      } yield a + b
      program.interpret[Option] shouldBe Option(2)
      program.interpret[List] shouldBe List(2)
    }

    "allow multiple args in smart constructors" in {
      @free
      trait MultiArgs {
        def x(a: Int, b: Int, c: Int): FS[Int]
      }
    }

    "allow smart constructors with no args" in {
      @free
      trait NoArgs {
        def x: FS[Int]
      }
    }

    "generate ADTs with friendly names and expose them as dependent types" in {
      @free
      trait FriendlyFreeS {
        def sc1(a: Int, b: Int, c: Int): FS[Int]
        def sc2(a: Int, b: Int, c: Int): FS[Int]
      }
      implicitly[FriendlyFreeS.Op[_] =:= FriendlyFreeS.Op[_]]
      implicitly[FriendlyFreeS.Sc1OP <:< FriendlyFreeS.Op[Int]]
      implicitly[FriendlyFreeS.Sc2OP <:< FriendlyFreeS.Op[Int]]
      ()
    }

    "allow smart constructors with type arguments" in {
      @free
      trait KVStore {
        def put[A](key: String, value: A): FS[Unit]
        def get[A](key: String): FS[Option[A]]
        def delete(key: String): FS[Unit]
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
        def x(key: String): FS[String]
        def y(key: String): FS[String]
        def z(key: String): FS[String]
      }
      implicit val interpreter = new ApplicativesServ.Handler[Option] {
        override def x(key: String): Option[String] = Some(key)
        override def y(key: String): Option[String] = Some(key)
        override def z(key: String): Option[String] = Some(key)
      }
      val v = ApplicativesServ[ApplicativesServ.Op]
      import v._
      val program = (x("a") |@| y("b") |@| z("c")).map { _ + _ + _ }.freeS
      program.interpret[Option] shouldBe Some("abc")
    }

    "allow sequential evaluation of combined FreeS & FreeS.Par" in {
      @free
      trait MixedFreeS {
        def x(key: String): FS[String]
        def y(key: String): FS[String]
        def z(key: String): FS[String]
      }
      implicit val interpreter = new MixedFreeS.Handler[Option] {
        override def x(key: String): Option[String] = Some(key)
        override def y(key: String): Option[String] = Some(key)
        override def z(key: String): Option[String] = Some(key)
      }
      val v = MixedFreeS[MixedFreeS.Op]
      import v._
      val apProgram = (x("a") |@| y("b")).map { _ + _ }
      val program = for {
        n <- z("1")
        m <- apProgram.freeS
      } yield n + m
      program.interpret[Option] shouldBe Some("1ab")
    }

    "allow non-FreeS concrete definitions in the trait" in {
      @free trait WithExtra {
        def x(a: Int): FS[String]
        def y: Int = 5
        val z: Int = 6
      }
      val v = WithExtra[WithExtra.Op]
      v.y shouldBe 5
      v.z shouldBe 6

      implicit val interpreter = new WithExtra.Handler[Id]{
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
      implicit val interpreter = new Combine.Handler[Id]{
        override def x(a: Int): Int = 4
      }
      v.y(5).interpret[Id] shouldBe(true)
    }

  }

}
