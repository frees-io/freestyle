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

package freestyle.asyncMonix

import org.scalatest._

import freestyle._
import freestyle.implicits._
import freestyle.async._
import freestyle.async.implicits._
import freestyle.asyncMonix.implicits._

import monix.eval.Task
import monix.cats._
import monix.execution.Scheduler

class AsyncMonixTests extends AsyncWordSpec with Matchers {
  implicit override def executionContext = Scheduler.Implicits.global

  "Async Monix Freestyle integration" should {
    "support Task as the target runtime" in {
      def program[F[_]: AsyncM] =
        for {
          a <- FreeS.pure(1)
          b <- AsyncM[F].async[Int](cb => cb(Right(42)))
          c <- FreeS.pure(1)
          d <- AsyncM[F].async[Int](cb => cb(Right(10)))
        } yield a + b + c + d

      program[AsyncM.Op].interpret[Task].runAsync map { _ shouldBe 54 }
    }
  }
}
