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

import cats.Eq
import cats.instances.int._
import cats.instances.string._
import cats.instances.tuple._
import cats.syntax.eq._
import cats.syntax.either._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.{ApplicativeTests, FunctorTests, MonadTests}
import org.scalatest.{FunSuite, Matchers}
import org.typelevel.discipline.scalatest.Discipline
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import freestyle.nondeterminism._

class NonDeterminismTests extends FunSuite with Discipline with Matchers {

  // Eq[Future[A]] / Eq[Throwable] -> cats/jvm/src/test/scala/cats/tests/FutureTests.scala

  val timeout = 3.seconds

  def futureEither[A](f: Future[A]): Future[Either[Throwable, A]] =
    f.map(Either.right[Throwable, A]).recover { case t => Either.left(t) }

  implicit def eqfa[A: Eq]: Eq[Future[A]] =
    new Eq[Future[A]] {
      def eqv(fx: Future[A], fy: Future[A]): Boolean = {
        val fz = futureEither(fx) zip futureEither(fy)
        Await.result(fz.map { case (tx, ty) => tx === ty }, timeout)
      }
    }

  implicit val throwableEq: Eq[Throwable] = new Eq[Throwable] {
    override def eqv(x: Throwable, y: Throwable): Boolean = x.toString == y.toString
  }

  checkAll("FutureNondeterminism", FunctorTests[Future].functor[Int, Int, Int])

  // fails
  //  - flatMap consistent apply
  //  - ap consistent with product + map
  // checkAll("FutureNondeterminism", ApplicativeTests[Future].applicative[Int, Int, Int])
  // checkAll("FutureNondeterminism", MonadTests[Future].monad[Int, Int, Int])

}
