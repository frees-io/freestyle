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

import cats.syntax.either._
import fs2.Task
import fs2.interop.cats._
import org.scalatest._
import _root_.doobie.imports._
import _root_.doobie.h2.h2transactor._

import freestyle.implicits._
import freestyle.doobie._
import freestyle.doobie.implicits._

import scala.language.postfixOps

class DoobieTests extends AsyncWordSpec with Matchers {

  import algebras._

  implicit val xa: Transactor[Task] =
    H2Transactor[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "").unsafeRunSync.toOption
      .getOrElse(throw new Exception("Could not create test transactor"))

  val query: ConnectionIO[Int] = sql"SELECT 1 + 1".query[Int].unique

  "Doobie Freestyle integration" should {

    "allow a doobie ConnectionIO program to be interleaved inside a program monadic flow" in {
      val program = for {
        a <- app.nonDoobie.x
        b <- app.doobieM.transact(query).freeS
        c <- FreeS.pure(1)
      } yield a + b + c
      program.exec[Task] map { _ shouldBe 4 } unsafeRunAsyncFuture
    }

    "allow doobie syntax to lift to FreeS" in {
      val program: FreeS[App.Op, Int] = for {
        a <- app.nonDoobie.x
        b <- query.liftFS[App.Op]
        c <- app.nonDoobie.x
      } yield a + b + c
      program.exec[Task] map { _ shouldBe 4 } unsafeRunAsyncFuture
    }

    "allow doobie syntax to lift to FreeS.Par" in {
      val program: FreeS[App.Op, Int] = for {
        a <- app.nonDoobie.x
        b <- query.liftFSPar[App.Op].freeS
        c <- app.nonDoobie.x
      } yield a + b + c
      program.exec[Task] map { _ shouldBe 4 } unsafeRunAsyncFuture
    }
  }

}

object algebras {
  @free
  trait NonDoobie {
    def x: OpSeq[Int]
  }

  implicit def nonDoobieHandler: NonDoobie.Handler[Task] =
    new NonDoobie.Handler[Task] {
      def x: Task[Int] = Task.now(1)
    }

  @module
  trait App[F[_]] {
    val nonDoobie: NonDoobie[F]
    val doobieM: DoobieM[F]
  }

  val app = App[App.Op]
}
