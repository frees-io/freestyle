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

import scala.concurrent._

import cats.Monad

import freestyle._
import freestyle.implicits._

import freestyle.http.play._

import _root_.play.api.mvc._
import _root_.play.api.http._
import _root_.play.api.test._

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

class PlayTests extends AsyncWordSpec with Matchers {
  implicit override def executionContext = ExecutionContext.Implicits.global

  // play

  implicit def unitWr(implicit C: Codec): Writeable[Unit] =
    Writeable(data => C.encode(data.toString))

  implicit val unitCT: ContentTypeOf[Unit] = new ContentTypeOf(Option("text/plain"))

  // akka

  implicit val actorSys: ActorSystem      = ActorSystem("test")
  implicit val materializer: Materializer = ActorMaterializer()

  "Play integration" should {
    import cats.instances.future._

    import algebras._
    import handlers._

    def program[F[_]: Noop]: FreeS[F, Result] =
      for {
        x <- Noop[F].noop
      } yield Results.Ok(x)

    "FreeSAction creates an action from a program" in {
      FreeSAction { program[Noop.Op] } shouldBe an[Action[Result]]
    }

    "FreeSAction creates an action from a function that returns a program given a request" in {
      FreeSAction { request =>
        Noop[Noop.Op].noop.map(_ => Results.Ok(request.method))
      } shouldBe an[Action[Result]]
    }

    "The resulting action writes the result in the response" in {
      import Helpers._

      val action: EssentialAction  = FreeSAction { program[Noop.Op] }
      val request                  = FakeRequest()
      val response: Future[Result] = Helpers.call(action, request)

      status(response) shouldBe 200
      contentAsString(response) shouldBe "()"
    }
  }
}

object algebras {
  @free
  trait Noop[F[_]] {
    def noop: FreeS[F, Unit]
  }
}

object handlers {
  import algebras._

  implicit def noopHandler[M[_]](
      implicit MM: Monad[M]
  ): Noop.Handler[M] = new Noop.Handler[M] {
    def noop: M[Unit] = MM.pure(())
  }
}
