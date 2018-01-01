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

package freestyle.free.http

import org.scalatest.{AsyncWordSpec, Matchers}

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._

import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.http.http4s._

class Http4sTests extends AsyncWordSpec with Matchers {
  import algebras._
  import handlers._
  implicit val x: FSHandler[App.Op, IO] = iota.CopK.FunctionK.summon

  "Http4s Freestyle integration" should {

    def getUser[F[_]: App](id: Long): FreeS[F, User] =
      App[F].userRepo.get(id)

    def getUsers[F[_]: App]: FreeS[F, List[User]] =
      App[F].userRepo.list

    "provide a EntityEncoder for FreeS types" in {
      "EntityEncoder[IO, FreeS[App.Op, String]]" should compile
    }

    "provide a EntityEncoder for FreeS.Par types" in {
      "EntityEncoder[IO, FreeS.Par[App.Op, String]]" should compile
    }
    "allow a FreeS program to be used with http4s" in {

      val userService = HttpService[IO] {
        case GET -> Root / "user" / LongVar(id) =>
          Ok(getUser[App.Op](id).map(_.toString))
        case GET -> Root / "users" =>
          Ok(getUsers[App.Op].map(_.mkString("\n")))
      }

      val reqUser1 = Request[IO](Method.GET, uri("/user/1"))

      (for {
        resp <- new syntax.KleisliResponseOps(userService).orNotFound(reqUser1)
        body <- EntityDecoder.decodeString(resp)
      } yield {
        resp.status shouldBe (Status.Ok)
        body should startWith("User")
      }).unsafeToFuture

      val reqUsers = Request[IO](Method.GET, uri("/users"))

      (for {
        resp <- new syntax.KleisliResponseOps(userService).orNotFound(reqUsers)
        body <- EntityDecoder.decodeString(resp)
      } yield {
        resp.status shouldBe (Status.Ok)
        all(body.split("\n")) should startWith("User")
      }).unsafeToFuture
    }

  }
}

object algebras {
  case class User(name: String)

  @free
  trait UserRepository {
    def get(id: Long): FS[User]
    def list: FS[List[User]]
  }

  @module
  trait App {
    val userRepo: UserRepository
  }
}

object handlers {
  import algebras._

  val users: Map[Long, User] = Map(
    1L -> User("foo"),
    2L -> User("bar")
  )

  implicit val userRepoHandler: UserRepository.Handler[IO] =
    new UserRepository.Handler[IO] {
      def get(id: Long): IO[User] =
        IO.pure(users.get(id).getOrElse(User("default")))
      def list: IO[List[User]] =
        IO.pure(users.values.toList)
    }
}
