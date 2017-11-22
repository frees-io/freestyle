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

import freestyle.async.implicits._
import freestyle.async.{AsyncContext, Future2AsyncM, Proc}
import org.scalatest._
import cats.effect.{Effect, IO}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Future2AsyncMTests extends WordSpec with Matchers {

  implicit def catsEffectAsyncContext[F[_]](implicit F: Effect[F]): AsyncContext[F] =
    new AsyncContext[F] {
      def runAsync[A](fa: Proc[A]): F[A] = F.async(fa)
    }

  val exception: Throwable = new RuntimeException("Future2AsyncMTest exception")

  def failedFuture[T]: Future[T] = Future.failed(exception)

  def successfulFuture[T](value: T): Future[T] = Future.successful(value)

  val F2F: Future2AsyncM[IO] = new Future2AsyncM[IO]

  val foo = "bar"

  "Future2Async Handler" should {

    "transform scala.concurrent.Future to cats.effect.IO successfully" in {
      F2F.apply(successfulFuture(foo)).map(_ shouldBe foo)
    }

    "recover from failed Futures and throw them accordingly" in {
      F2F.apply(failedFuture[String]).attempt.map(_ shouldBe Left(exception))
    }

  }
}
