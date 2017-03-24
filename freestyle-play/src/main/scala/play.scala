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

import freestyle._

import cats.Monad

import scala.concurrent._

import _root_.play.api.mvc._
import _root_.play.api.http._

object play {
  object FreeSAction {
    def apply[F[_]](prog: FreeS[F, Result])(
        implicit MF: Monad[Future],
        I: ParInterpreter[F, Future],
        EC: ExecutionContext
    ): Action[AnyContent] = {
      Action.async {
        prog.exec[Future]
      }
    }

    def apply[A, F[_]](fn: Request[AnyContent] => FreeS[F, Result])(
        implicit MF: Monad[Future],
        I: ParInterpreter[F, Future],
        EC: ExecutionContext
    ): Action[AnyContent] = {
      Action.async { request =>
        val prog = fn(request)
        prog.exec[Future]
      }
    }
  }
}
