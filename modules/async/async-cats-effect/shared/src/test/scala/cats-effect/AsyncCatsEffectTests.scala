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

package freestyle.free.asyncCatsEffect

import cats.effect.IO
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.async._
import freestyle.free.async.implicits._
import freestyle.asyncCatsEffect.implicits._
import org.scalatest.{AsyncWordSpec, Matchers}
import scala.concurrent.ExecutionContext

class AsyncFs2Tests extends AsyncWordSpec with Matchers {

  implicit override def executionContext = ExecutionContext.Implicits.global

  "Async Cats Effect Freestyle integration" should {
    "support IO as the target runtime" in {
      def program[F[_]: AsyncM] =
        for {
          a <- FreeS.pure(1)
          b <- AsyncM[F].async[Int](cb => cb(Right(a + 41)))
          c <- FreeS.pure(1)
          d <- AsyncM[F].async[Int](cb => cb(Right(c + 9)))
        } yield a + b + c + d

      program[AsyncM.Op].interpret[IO].unsafeToFuture map { _ shouldBe 54 }
    }
  }
}
