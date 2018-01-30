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

package freestyle.tagless

import org.scalatest.{Matchers, WordSpec}

import freestyle.free._

import cats.{~>, Functor, Monad}
import cats.arrow.FunctionK
import cats.instances.either._
import cats.instances.option._
import cats.kernel.instances.int._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._

object algebras {

  @tagless @stacksafe
  trait TG1 {
    def x(a: Int): FS[Int]

    def y(a: Int): FS[Int]
  }

  @tagless @stacksafe
  trait TG2 {
    def x2(a: Int): FS[Int]

    def y2(a: Int): FS[Int]
  }

  @tagless @stacksafe
  trait TG3[F[_]] {
    def x3(a: Int): FS[Int]

    def y3(a: Int): FS[Int]
  }

  @free
  trait F1 {
    def a(a: Int): FS[Int]

    def b(a: Int): FS[Int]
  }

}

object handlers  {

  import algebras._

  implicit val optionHandler1: TG1.Handler[Option] = new TG1.Handler[Option] {
    def x(a: Int): Option[Int] = Some(a)
    def y(a: Int): Option[Int] = Some(a)
  }

  implicit val optionHandler2: TG2.Handler[Option] = new TG2.Handler[Option] {
    def x2(a: Int): Option[Int] = Some(a)
    def y2(a: Int): Option[Int] = Some(a)
  }

  implicit val optionHandler3: TG3.Handler[Option] = new TG3.Handler[Option] {
    def x3(a: Int): Option[Int] = Some(a)
    def y3(a: Int): Option[Int] = Some(a)
  }

  implicit val f1OptionHandler1: F1.Handler[Option] = new F1.Handler[Option] {
    def a(a: Int): Option[Int] = Some(a)
    def b(a: Int): Option[Int] = Some(a)
  }

  implicit def mIdentity[F[_]]: F ~> F = FunctionK.id[F]

}

object utils {

  import algebras._

  val iterations = 50000

  def SOProgram[F[_] : Monad : TG1](i: Int): F[Int] = for {
    j <- TG1[F].x(i + 1)
    z <- if (j < iterations) SOProgram[F](j) else Monad[F].pure(j)
  } yield z

}