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

package freestyle.http

import org.scalatest.{AsyncWordSpec, Matchers}

import fs2.Task
import fs2.interop.cats._
import org.http4s._
import org.http4s.dsl._

import freestyle._
import freestyle.implicits._
import freestyle.http.http4s._

class Http4sTests extends AsyncWordSpec with Matchers {
  import algebras._
  import handlers._

  "Http4s Freestyle integration" should {

    def getUser[F[_]: App](id: Long): FreeS[F, User] =
      App[F].userRepo.get(id)

    def getUsers[F[_]: App]: FreeS[F, List[User]] =
      App[F].userRepo.list

    "provide a EntityEncoder for FreeS types" in {
      "EntityEncoder[FreeS[App.Op, String]]" should compile
      "EntityEncoder[FreeS.Par[App.Op, String]]" should compile
    }

    "allow a FreeS program to be used with http4s" in {
      val userService = HttpService {
        case GET -> Root / "user" / LongVar(id) =>
          Ok(getUser[App.Op](id).map(_.toString))
        case GET -> Root / "users" =>
          Ok(getUsers[App.Op].map(_.mkString("\n")))
      }

      val reqUser1 = Request(Method.GET, uri("/user/1"))

      (for {
        resp <- userService.orNotFound(reqUser1)
        body <- EntityDecoder.decodeString(resp)
      } yield {
        resp.status shouldBe (Status.Ok)
        body should startWith("User")
      }).unsafeRunAsyncFuture

      val reqUsers = Request(Method.GET, uri("/users"))

      (for {
        resp <- userService.orNotFound(reqUsers)
        body <- EntityDecoder.decodeString(resp)
      } yield {
        resp.status shouldBe (Status.Ok)
        all(body.split("\n")) should startWith("User")
      }).unsafeRunAsyncFuture
    }
  }
}

object algebras {
  case class User(name: String)

  @free
  trait UserRepository {
    def get(id: Long): OpPar[User]
    def list: OpPar[List[User]]
  }

  @module
  trait App[F[_]] {
    val userRepo: UserRepository[F]
  }
}

object handlers {
  import algebras._

  val users: Map[Long, User] = Map(
    1L -> User("foo"),
    2L -> User("bar")
  )

  implicit val userRepoHandler: UserRepository.Handler[Task] =
    new UserRepository.Handler[Task] {
      def get(id: Long): Task[User] =
        Task.now(users.get(id).getOrElse(User("default")))
      def list: Task[List[User]] =
        Task.now(users.values.toList)
    }
}
