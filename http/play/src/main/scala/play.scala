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

import cats.instances.future._
import freestyle._
import scala.concurrent.{ExecutionContext, Future}

package play {

  object implicits {

    implicit def seqToFuture[F[_], A](prog: FreeS[F, A])(
        implicit I: ParInterpreter[F, Future],
        EC: ExecutionContext
    ): Future[A] = prog.parExec[Future]

    implicit def parSeqToFuture[F[_], A](prog: FreeS[F, A])(
        implicit I: FSHandler[F, Future],
        EC: ExecutionContext
    ): Future[A] = prog.exec[Future]

    implicit def parToFuture[F[_], A](prog: FreeS.Par[F, A])(
        implicit I: FSHandler[F, Future],
        EC: ExecutionContext
    ): Future[A] = prog.exec[Future]

  }

}
