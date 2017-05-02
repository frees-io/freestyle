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

import _root_.slick.dbio.{DBIO, DBIOAction}
import _root_.slick.jdbc.JdbcBackend
import _root_.slick.jdbc.H2Profile.api._

import org.scalatest.{AsyncWordSpec, Matchers}

import freestyle.implicits._
import freestyle.slick._
import freestyle.slick.implicits._
import cats.implicits._

import scala.concurrent.Future

class SlickTests extends AsyncWordSpec with Matchers {

  import algebras._

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  implicit val db = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver")

  val query: DBIO[Int] = sql"SELECT 1 + 1".as[Int].head

  "Slick Freestyle integration" should {

    "allow a Slick DBIO program to be interleaved inside a program monadic flow" in {
      val program = for {
        a <- app.nonSlick.x
        b <- app.slickM.run(query).freeS
        c <- FreeS.pure(1)
      } yield a + b + c
      program.interpret[Future] map { _ shouldBe 4 }
    }

    "allow slick syntax to lift to FreeS" in {
      val program: FreeS[App.Op, Int] = for {
        a <- app.nonSlick.x
        b <- query.liftFS[App.Op]
        c <- app.nonSlick.x
      } yield a + b + c
      program.interpret[Future] map { _ shouldBe 4 }
    }

    "allow slick syntax to lift to FreeS.Par" in {
      val program: FreeS[App.Op, Int] = for {
        a <- app.nonSlick.x
        b <- query.liftFSPar[App.Op].freeS
        c <- app.nonSlick.x
      } yield a + b + c
      program.interpret[Future] map { _ shouldBe 4 }
    }
  }

}

object algebras {
  @free
  trait NonSlick {
    def x: FS[Int]
  }

  implicit def nonSlickHandler: NonSlick.Handler[Future] =
    new NonSlick.Handler[Future] {
      def x: Future[Int] = Future.successful(1)
    }

  @module
  trait App {
    val nonSlick: NonSlick
    val slickM: SlickM
  }

  val app = App[App.Op]
}
