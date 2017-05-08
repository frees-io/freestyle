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

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext
import cats.Monad
import freestyle._
import freestyle.implicits._
import freestyle.http.play.implicits._
import _root_.play.api.mvc.{Action, Codec, Request, Result, Results}
import _root_.play.api.http.{ContentTypeOf, Writeable}
import ExecutionContext.Implicits.global

class PlayTests extends WordSpec with Matchers {

  implicit def unitWr(implicit C: Codec): Writeable[Unit] =
    Writeable(data => C.encode(data.toString))

  implicit val unitCT: ContentTypeOf[Unit] = new ContentTypeOf(Option("text/plain"))

  "Play integration" should {
    import cats.instances.future._

    import algebras._
    import handlers._

    def program[F[_]: Noop]: FreeS[F, Result] =
      Noop[F].noop.map(x => Results.Ok(x))

    "FreeS programs can be used as return value in Play actions" in {
      Action.async { _ =>
        program[Noop.Op]
      } shouldBe an[Action[Result]]
    }

    "FreeS.Par programs can interact with a given request and used as returned values in Play actions" in {
      Action.async { request =>
        Noop[Noop.Op].noop.map(_ => Results.Ok(request.method))
      } shouldBe an[Action[Result]]
    }

    "FreeS sequentials programs can interact with a given request and used as returned values in Play actions" in {
      Action.async { request =>
        Noop[Noop.Op].noop.map(_ => Results.Ok(request.method)).freeS
      } shouldBe an[Action[Result]]
    }
  }
}

object algebras {
  @free
  trait Noop {
    def noop: FS[Unit]
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
