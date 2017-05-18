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

import cats.arrow.FunctionK
import cats.implicits._
import org.scalatest.{Matchers, WordSpec}

class moduleTests extends WordSpec with Matchers {

  "the @module annotation" should {

    import freestyle.implicits._
    import interps._

    "be rejected if applied to a non-abstract class" in {
      """@module class Foo[F[_]] { val x: Int}""" shouldNot compile
    }

    "be rejected if applied to a trait with companion object" in {
      """@module trait  Foo[F[_]] { val x: Int} ; object Foo """ shouldNot compile
    }


    "[simple] create a companion with a `T` type alias" in {
      type T[A] = M1.Op[A]
    }

    "[onion] create a companion with a `T` type alias" in {
      type T[A] = O1.Op[A]
    }

    "[simple] provide instances through it's companion `apply`" in {
      "M1[M1.Op]" should compile
    }

    "[onion] provide instances through it's companion `apply`" in {
      "O1[O1.Op]" should compile
    }

    "[simple] implicit summoning" in {
      "implicitly[M1[M1.Op]]" should compile
    }

    "[onion] allow implicit sumoning" in {
      "implicitly[O1[O1.Op]]" should compile
    }

    "[simple] autowire implementations of it's contained smart constructors" in {
      val m1 = M1[M1.Op]
      "(m1.sctors1: SCtors1[M1.Op])" should compile
      "(m1.sctors2: SCtors2[M1.Op])" should compile
    }

    "[onion] autowire implementations of it's contained smart constructors" in {
      val o1 = O1[O1.Op]
      "(o1.m1.sctors1: SCtors1[O1.Op])" should compile
      "(o1.m1.sctors2: SCtors2[O1.Op])" should compile
      "(o1.m2.sctors3: SCtors3[O1.Op])" should compile
      "(o1.m2.sctors4: SCtors4[O1.Op])" should compile
    }

    "[simple] allow composition of it's contained algebras" in {
      val m1 = M1[M1.Op]
      val result = for {
        a <- m1.sctors1.x(1)
        b <- m1.sctors1.y(1)
        c <- m1.sctors2.i(1)
        d <- m1.sctors2.j(1)
      } yield a + b + c + d
      "(result: FreeS[M1.Op, Int])" should compile
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
      "(result: FreeS[O1.Op, Int])" should compile
    }

    "[simple] find a FunctionK[Module.Op, ?] providing there is existing ones for it's smart constructors" in {
      import freestyle.implicits._
      import interps.{optionHandler1, optionHandler2}
      "implicitly[FunctionK[M1.Op, Option]]" should compile
      "implicitly[FSHandler[M1.Op, Option]]" should compile
    }

    "[onion] find a FunctionK[Module.Op, ?] providing there is existing ones for it's smart constructors" in {
      "implicitly[FSHandler[O1.Op, Option]]" should compile
      "implicitly[FunctionK[O1.Op, Option]]" should compile
    }

    "[simple] reuse program interpretation in diferent runtimes" in {
      val m1 = M1[M1.Op]
      val program = for {
        a <- m1.sctors1.x(1)
        b <- m1.sctors1.y(1)
        c <- m1.sctors2.i(1)
        d <- m1.sctors2.j(1)
      } yield a + b + c + d
      program.interpret[Option] shouldBe Option(4)
      program.interpret[List] shouldBe List(4)
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

      program.interpret[Option] shouldBe Option(8)
      program.interpret[List] shouldBe List(8)
    }

    "pass through concrete members to implementations" in {
      val o2 = O2[O2.Op]
      o2.x shouldBe 1
      o2.y shouldBe 2
    }

    "allow modules with just concrete members unrelated to freestyle's concerns" in {
      val o3 = O3[O3.Op]
      o3.x shouldBe 1
      o3.y shouldBe 2
    }

    "allow modules be composed with algebras, having methods returning FS effects" in {
      import comp._
      val m1 = FSMod[FSMod.Op]
      val program = for {
        a <- m1.x(1)
        b <- m1.y(1)
      } yield a + b
      program.interpret[Option] shouldBe Option(2)
    }
  }

}
