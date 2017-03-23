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
import freestyle.async._

import scala.util.{Failure, Success}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object slick {

  @free sealed trait SlickM {
    def run[A](f: DBIO[A]): OpPar[A]
  }

  object implicits {
    implicit def freeStyleSlickHandler[M[_]](
        implicit asyncContext: AsyncContext[M],
        db: JdbcBackend#DatabaseDef): SlickM.Handler[M] =
      new SlickM.Handler[M] {
        def run[A](fa: DBIO[A]): M[A] = asyncContext.runAsync { cb =>
          db.run(fa).onComplete {
            case Success(x) => cb(Right(x))
            case Failure(e) => cb(Left(e))
          }
        }
      }

    implicit def freeStyleSlickFutureHandler(
        implicit db: JdbcBackend#DatabaseDef): SlickM.Handler[Future] =
      new SlickM.Handler[Future] {
        def run[A](fa: DBIO[A]): Future[A] = db.run(fa)
      }

    implicit def freeSLiftSlick[F[_]: SlickM]: FreeSLift[F, DBIO] =
      new FreeSLift[F, DBIO] {
        def liftFSPar[A](dbio: DBIO[A]): FreeS.Par[F, A] = SlickM[F].run(dbio)
      }
  }

}
