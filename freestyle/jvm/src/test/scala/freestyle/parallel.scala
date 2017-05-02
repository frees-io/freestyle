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

import cats.data.Kleisli
import cats.implicits._
import monix.eval.Task
import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class parallelTests extends WordSpec with Matchers {

  "Applicative Parallel Support" should {

    class NonDeterminismTestShared {
      import freestyle.nondeterminism._
      import freestyle.implicits._

      val buf = scala.collection.mutable.ArrayBuffer.empty[Int]

      def blocker(value: Int, waitTime: Long): Int = {
        Thread.sleep(waitTime)
        buf += value
        value
      }

      val v = MixedFreeS[MixedFreeS.Op]
      import v._

      val program = for {
        a  <- z //3
        bc <- (x |@| y).tupled.freeS //(1,2)
        (b, c) = bc
        d <- z //3
      } yield a :: b :: c :: d :: Nil // List(3,1,2,3)

    }

    "allow non deterministic execution when interpreting to scala.concurrent.Future" in {
      import freestyle.nondeterminism._
      import freestyle.implicits._

      import scala.concurrent.ExecutionContext.Implicits.global

      val test = new NonDeterminismTestShared
      import test._

      implicit val interpreter = new MixedFreeS.Handler[Future] {
        override def x: Future[Int] = Future(blocker(1, 1000L))
        override def y: Future[Int] = Future(blocker(2, 0L))
        override def z: Future[Int] = Future(blocker(3, 2000L))
      }

      Await.result(program.interpret[Future], Duration.Inf) shouldBe List(3, 1, 2, 3)
      buf.toArray shouldBe Array(3, 2, 1, 3)
    }

    "allow non deterministic execution when interpreting to monix.eval.Task" in {
      import freestyle.nondeterminism._
      import freestyle.implicits._

      import monix.cats._
      import monix.eval.Task.nondeterminism
      import monix.execution.Scheduler.Implicits.global

      val test = new NonDeterminismTestShared
      import test._

      implicit val interpreter = new MixedFreeS.Handler[Task] {
        override def x: Task[Int] = Task(blocker(1, 1000L))
        override def y: Task[Int] = Task(blocker(2, 0L))
        override def z: Task[Int] = Task(blocker(3, 2000L))
      }

      Await.result(program.interpret[Task].runAsync, Duration.Inf) shouldBe List(3, 1, 2, 3)
      buf.toArray shouldBe Array(3, 2, 1, 3)
    }

    "allow deterministic programs with FreeS.Par nodes run deterministically" in {
      import freestyle.nondeterminism._
      import freestyle.implicits._

      val test = new NonDeterminismTestShared
      import test._

      implicit val interpreter = new MixedFreeS.Handler[Option] {
        override def x: Option[Int] = Option(blocker(1, 1000L))
        override def y: Option[Int] = Option(blocker(2, 0L))
        override def z: Option[Int] = Option(blocker(3, 2000L))
      }

      program.interpret[Option] shouldBe Option(List(3, 1, 2, 3))
      buf.toArray shouldBe Array(3, 1, 2, 3)
    }

    /**
     * Similar example as the one found at
     * http://typelevel.org/cats/datatypes/freeapplicative.html
     */
    "allow validation style algebras derived from FreeS.Par" in {
      import cats.implicits._
      import scala.concurrent.ExecutionContext.Implicits.global

      import freestyle.nondeterminism._
      import freestyle.implicits._

      type ParValidator[A] = Kleisli[Future, String, A]

      @free
      trait Validation {
        def minSize(n: Int): FS[Boolean]
        def hasNumber: FS[Boolean]
      }

      implicit val interpreter = new Validation.Handler[ParValidator] {
        override def minSize(n: Int): ParValidator[Boolean] =
          Kleisli(s => Future(s.size >= n))
        override def hasNumber: ParValidator[Boolean] =
          Kleisli(s => Future(s.exists(c => "0123456789".contains(c))))
      }

      val validation = Validation[Validation.Op]
      import validation._

      val parValidation = (minSize(3) |@| hasNumber).map(_ :: _ :: Nil)
      val validator     = parValidation.interpret[ParValidator]

      Await.result(validator.run("a"), Duration.Inf) shouldBe List(false, false)
      Await.result(validator.run("abc"), Duration.Inf) shouldBe List(true, false)
      Await.result(validator.run("abc1"), Duration.Inf) shouldBe List(true, true)
      Await.result(validator.run("1a"), Duration.Inf) shouldBe List(false, true)
    }
  }

}
