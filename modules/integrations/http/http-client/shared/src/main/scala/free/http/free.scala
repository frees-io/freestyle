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

import _root_.hammock._
import cats.{MonadError, ~>}
import cats.effect.Sync
import cats.free.Free

object client {
  import freestyle.free._
  import freestyle.free.implicits._

  @free sealed trait HammockM {
    def options(uri: Uri, headers: Map[String, String]): FS[HttpResponse]
    def get(uri: Uri, headers: Map[String, String]): FS[HttpResponse]
    def head(uri: Uri, headers: Map[String, String]): FS[HttpResponse]
    def post(uri: Uri, headers: Map[String, String], body: Option[Entity]): FS[HttpResponse]
    def put(uri: Uri, headers: Map[String, String], body: Option[Entity]): FS[HttpResponse]
    def delete(uri: Uri, headers: Map[String, String]): FS[HttpResponse]
    def trace(uri: Uri, headers: Map[String, String]): FS[HttpResponse]
    def run[A](req: Free[HttpF, A]): FS[A]
  }

  trait Implicits {
    implicit def freeStyleHammockHandler[M[_] : MonadError[?[_], Throwable]](implicit I: InterpTrans[M], S: Sync[M]): HammockM.Handler[M] =
      new HammockM.Handler[M] {
        def options(uri: Uri, headers: Map[String, String]): M[HttpResponse] = Ops.options(uri, headers) foldMap I.trans
        def get(uri: Uri, headers: Map[String, String]): M[HttpResponse] = Ops.get(uri, headers) foldMap I.trans
        def head(uri: Uri, headers: Map[String, String]): M[HttpResponse] = Ops.head(uri, headers) foldMap I.trans
        def post(uri: Uri, headers: Map[String, String], body: Option[Entity]): M[HttpResponse] = Ops.post(uri, headers, body) foldMap I.trans
        def put(uri: Uri, headers: Map[String, String], body: Option[Entity]): M[HttpResponse] = Ops.put(uri, headers, body) foldMap I.trans
        def delete(uri: Uri, headers: Map[String, String]): M[HttpResponse] = Ops.delete(uri, headers) foldMap I.trans
        def trace(uri: Uri, headers: Map[String, String]): M[HttpResponse] = Ops.trace(uri, headers) foldMap I.trans
        def run[A](req: Free[HttpF, A]): M[A] = req foldMap I.trans
      }

    implicit def freeSLiftHammock[F[_]: HammockM]: FreeSLift[F, Free[HttpF, ?]] =
      new FreeSLift[F, Free[HttpF, ?]] {
        def liftFSPar[A](hio: Free[HttpF, A]): FreeS.Par[F, A] = HammockM[F].run(hio)
      }
  }

  object implicits extends Implicits
}
