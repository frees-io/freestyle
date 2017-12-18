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

package freestyle.free

import cats.effect.IO
import cats.syntax.either._
import org.scalatest._
import _root_.doobie._
import _root_.doobie.implicits._
import _root_.doobie.h2.H2Transactor

import freestyle.free.implicits._
import freestyle.free.doobie._
import freestyle.free.doobie.implicits._

import scala.language.postfixOps

class DoobieTests extends AsyncWordSpec with Matchers {

  import algebras._

  implicit val xa: Transactor[IO] =
    H2Transactor[IO]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "").unsafeRunSync

  val query: ConnectionIO[Int] = sql"SELECT 1 + 1".query[Int].unique

  "Doobie Freestyle integration" should {

    "allow a doobie ConnectionIO program to be interleaved inside a program monadic flow" in {
      val program = for {
        a <- app.nonDoobie.x
        b <- app.doobieM.transact(query).freeS
        c <- FreeS.pure(1)
      } yield a + b + c
      program.interpret[IO] map { _ shouldBe 4 } unsafeToFuture
    }

    "allow doobie syntax to lift to FreeS" in {
      val program: FreeS[App.Op, Int] = for {
        a <- app.nonDoobie.x
        b <- query.liftFS[App.Op]
        c <- app.nonDoobie.x
      } yield a + b + c
      program.interpret[IO] map { _ shouldBe 4 } unsafeToFuture
    }

    "allow doobie syntax to lift to FreeS.Par" in {
      val program: FreeS[App.Op, Int] = for {
        a <- app.nonDoobie.x
        b <- query.liftFSPar[App.Op].freeS
        c <- app.nonDoobie.x
      } yield a + b + c
      program.interpret[IO] map { _ shouldBe 4 } unsafeToFuture
    }
  }

}

object algebras {
  @free
  trait NonDoobie {
    def x: FS[Int]
  }

  implicit def nonDoobieHandler: NonDoobie.Handler[IO] =
    new NonDoobie.Handler[IO] {
      def x: IO[Int] = IO.pure(1)
    }

  @module
  trait App {
    val nonDoobie: NonDoobie
    val doobieM: DoobieM
  }

  val app = App[App.Op]
}
