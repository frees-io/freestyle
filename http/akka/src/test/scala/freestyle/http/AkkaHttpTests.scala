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

package freestyle.http.akka

import _root_.akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import _root_.akka.http.scaladsl.model.StatusCodes.OK
import _root_.akka.http.scaladsl.server.Directives._
import _root_.akka.http.scaladsl.server.Route
import _root_.akka.http.scaladsl.server.PathMatchers.IntNumber
import _root_.akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.Id
import freestyle._
import freestyle.implicits._
import org.scalatest.{Matchers, WordSpec}

class AkkaHttpTests extends WordSpec with Matchers with ScalatestRouteTest {

  import userrepo._

  "Akka Http integration in  Freestyle" should {

    "provide a ToEntityMarshaller for FreeS types" in {
      "implicitly[ToEntityMarshaller[FreeS[UserApp.Op, String]]]" should compile
    }

    "provide a ToEntityMarshaller for FreeS.Par types" in {
      "implicitly[ToEntityMarshaller[FreeS.Par[UserApp.Op, String]]]" should compile
    }

    "allow a FreeS.Par program to be used with akka-http (1) " in {
      Get("/user/1") ~> userRoute.route ~> check {
        status shouldBe OK
        responseAs[String] shouldEqual "User(foo)"
      }
    }

    "allow a FreeS program to be used with akka-http (2) " in {
      Get("/users") ~> userRoute.route ~> check {
        status shouldBe OK
        responseAs[String] shouldEqual "Users(foo,bar)"
      }
    }

  }
}

case class User(name: String)

object userrepo {

  @free
  trait UserApp[F[_]] {
    def get(id: Int): FreeS.Par[F, User]
    def list: FreeS[F, List[User]]
  }

  implicit val handler: UserApp.Handler[Id] = new UserApp.Handler[Id] {
    private[this] val users: Map[Int, User] = Map(
      1 -> User("foo"),
      2 -> User("bar")
    )

    override def get(id: Int): User = users.get(id).getOrElse(User("default"))
    override def list: List[User]   = users.values.toList
  }

  val app = UserApp.to[UserApp.Op]
}

object userRoute {

  import userrepo._

  implicit val userMarshaller: ToEntityMarshaller[User] =
    Marshaller.StringMarshaller.compose((user: User) => s"User(${user.name})")

  implicit val usersMarshaller: ToEntityMarshaller[List[User]] =
    Marshaller.StringMarshaller.compose((users: List[User]) =>
      s"Users(${users.map(_.name).mkString(",")})")

  val route: Route =
    (get & path("user" / IntNumber)) { id =>
      complete(app.get(id))
    } ~
      (get & path("users")) { complete(app.list) }

}
