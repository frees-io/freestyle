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

import freestyle._
import freestyle.implicits._
import org.scalatest.{Matchers, WordSpec}
import cats._
import cats.implicits._

object taglessAlg {

  @tagless
  trait TG1 {
    def x(a: Int): FS[Int]
    def y(a: Int): FS[Int]
  }

  implicit val optionHandler1: TG1.TFHandler[Option] = new TG1.TFHandler[Option] {
    def x(a: Int): Option[Int] = Some(a)
    def y(a: Int): Option[Int] = Some(a)
  }

}

object freeAlg {

  @free
  trait F1 {
    def a(a: Int): FS[Int]
    def b(a: Int): FS[Int]
  }

  implicit val optionHandler1: F1.Handler[Option] = new F1.Handler[Option] {
    def a(a: Int): Option[Int] = Some(a)
    def b(a: Int): Option[Int] = Some(a)
  }

}

class taglessTests extends WordSpec with Matchers {

  import freeAlg._, taglessAlg._

  @module trait App[F[_]] {
    val f1 : F1[F]
    val ag : TG1.MonadSupportToFreeS[F]
  }


  "the @tagless annotation" should {

    "compile" in {

      def program[F[_] : F1: TG1.MonadSupportToFreeS]: FreeS[F, Int] = {
        val s = TG1
        val f = F1[F]
        for {
          a <- f.a(1)
          b <- f.b(1)
          x <- s.x(1)
          y <- s.y(1)
        } yield a + b + x + y
      }
      import TG1._
      program[App.Op].exec[Option] shouldBe Option(4)

    }

  }

}
