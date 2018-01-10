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

package freestyle.free
package http


import cats.{MonadError, ~>}
import cats.implicits._
import cats.effect._
import org.scalatest._
import _root_.hammock._

import freestyle.free.implicits._
import freestyle.free.http.client._
import freestyle.free.http.client.implicits._


class ClientTests extends WordSpec with Matchers {

  import stuff._

  implicit val interp = new InterpTrans[IO] {

    def trans(implicit S: Sync[IO]): HttpF ~> IO = new (HttpF ~> IO) {
      def apply[A](req: HttpF[A]): IO[A] = req match {
        case req@Options(uri) => doReq(uri, Method.OPTIONS)(S)
        case req@Get(uri) => doReq(uri, Method.GET)(S)
        case req@Head(uri) => doReq(uri, Method.HEAD)(S)
        case req@Post(uri) => doReq(uri, Method.POST)(S)
        case req@Put(uri) => doReq(uri, Method.PUT)(S)
        case req@Delete(uri) => doReq(uri, Method.DELETE)(S)
        case req@Trace(uri) => doReq(uri, Method.TRACE)(S)
      }
    }
  }


  private def doReq[F[_]](req: HttpRequest, method: Method)(implicit S: Sync[F]): F[HttpResponse] = S.catchNonFatal {
    HttpResponse(Status.OK, Map(), Entity.StringEntity(s"got a $method request to ${req.uri.show}"))
  }

  "Hammock integration" should {

    val uri = Uri.unsafeParse("http://test.com")
    val headers = Map.empty[String, String]
    val body = None
    Seq(("options", app.hammock.options(uri, headers)),
      ("get", app.hammock.get(uri, headers)),
      ("head", app.hammock.head(uri, headers)),
      ("post", app.hammock.post(uri, headers, body)),
      ("put", app.hammock.put(uri, headers, body)),
      ("delete", app.hammock.delete(uri, headers)),
      ("trace", app.hammock.trace(uri, headers))) foreach {
      case (method, op) =>
        s"allow the use of $method requests" in {
          val program = for {
            _ <- app.nonHammock.x
            a <- op
            content <- FreeS.pure(HttpResponse.entity.get(a))
          } yield content

          program.interpret[IO].unsafeRunSync shouldEqual Entity.StringEntity(s"got a ${method.toUpperCase} request to ${uri.show}")
        }
    }


    "allow a HttpRequestIO program to be lifted inside a monadic flow" in {
      val program = for {
        a <- app.nonHammock.x
        b <- app.hammock.run(Ops.get(Uri.unsafeParse("http://test.com"), Map()))
        content <- FreeS.pure(HttpResponse.entity.get(b))
      } yield content

      program.interpret[IO].unsafeRunSync shouldEqual Entity.StringEntity(s"got a GET request to ${uri.show}")
    }

    "allow a HttpRequestIO program to be lifted to FreeS" in {
      val program = for {
        a <- app.nonHammock.x
        b <- Ops.get(Uri.unsafeParse("http://test.com"), Map()).liftFS[App.Op]
        content <- FreeS.pure(HttpResponse.entity.get(b))
      } yield content

      program.interpret[IO].unsafeRunSync shouldEqual Entity.StringEntity(s"got a GET request to ${uri.show}")
    }

    "allow a HttpRequestIO program to be lifted to FreeS.Par" in {
      val program = for {
        a <- app.nonHammock.x
        b <- Ops.get(Uri.unsafeParse("http://test.com"), Map()).liftFSPar[App.Op].freeS
        content <- FreeS.pure(HttpResponse.entity.get(b))
      } yield content

      program.interpret[IO].unsafeRunSync shouldEqual Entity.StringEntity(s"got a GET request to ${uri.show}")
    }

  }
  
}


object stuff {

  import freestyle.free.http.client.implicits._

  @free
  trait NonHammock {
    def x: FS[Int]
  }

  implicit def nonHammockHandler: NonHammock.Handler[IO] =
    new NonHammock.Handler[IO] {
      def x: IO[Int] = IO(42)
    }

  @module
  trait App {
    val nonHammock: NonHammock
    val hammock: HammockM
  }

  val app = App[App.Op]

}
