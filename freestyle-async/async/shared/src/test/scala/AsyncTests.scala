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

import org.scalatest._

import cats.instances.future._

import freestyle._
import freestyle.implicits._
import freestyle.async._
import freestyle.async.implicits._

import scala.concurrent.{ExecutionContext, Future}

class AsyncTests extends AsyncWordSpec with Matchers {
  implicit override def executionContext = ExecutionContext.Implicits.global

  "Async Freestyle integration" should {
    "allow an Async to be interleaved inside a program monadic flow" in {
      def program[F[_]: AsyncM] =
        for {
          a <- FreeS.pure(1)
          b <- AsyncM[F].async[Int](cb => cb(Right(42)))
          c <- FreeS.pure(1)
        } yield a + b + c

      program[AsyncM.Op].interpret[Future] map { _ shouldBe 44 }
    }

    "allow multiple Async to be interleaved inside a program monadic flow" in {
      def program[F[_]: AsyncM] =
        for {
          a <- FreeS.pure(1)
          b <- AsyncM[F].async[Int](cb => cb(Right(42)))
          c <- FreeS.pure(1)
          d <- AsyncM[F].async[Int](cb => cb(Right(10)))
        } yield a + b + c + d

      program[AsyncM.Op].interpret[Future] map { _ shouldBe 54 }
    }

    case class OhNoException() extends Exception

    "allow Async errors to short-circuit a program" in {
      def program[F[_]: AsyncM] =
        for {
          a <- FreeS.pure(1)
          b <- AsyncM[F].async[Int](cb => cb(Left(OhNoException())))
          c <- FreeS.pure(3)
        } yield a + b + c

      program[AsyncM.Op].interpret[Future] recover { case OhNoException() => 42 } map { _ shouldBe 42 }
    }
  }
}
