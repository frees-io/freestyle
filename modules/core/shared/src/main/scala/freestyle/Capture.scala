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

import cats.Id
import simulacrum.typeclass

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import annotation.implicitNotFound

/*
 * The method `Applicative#pure` in `cats.Applicative` is strict on its parameter. Thus, it
 *  forces the evaluation of any expression passed to it.
 *
 * However, since we need to support different types in the Handlers, we need to
 *  define a `Capture` type-class..
 */
@typeclass
@implicitNotFound(msg = AnnotationMessages.captureInstanceNotFoundMsg)
trait Capture[F[_]] {
  def capture[A](a: => A): F[A]
}

object Capture extends CaptureInstances

trait CaptureInstances {

  implicit def freeStyleFutureCaptureInstance(implicit ec: ExecutionContext): Capture[Future] =
    new Capture[Future] {
      override def capture[A](a: => A): Future[A] = Future(a)
    }

  implicit val freeStyleIdCaptureInstance: Capture[Id] =
    new Capture[Id] {
      override def capture[A](a: => A): Id[A] = a
    }

  implicit val freeStyleTryCaptureInstance: Capture[Try] =
    new Capture[Try] {
      override def capture[A](a: => A): Try[A] = Try(a)
    }

}
