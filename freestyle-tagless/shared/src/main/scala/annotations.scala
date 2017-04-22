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

import cats.Monad

import scala.annotation.{StaticAnnotation, compileTimeOnly}

@compileTimeOnly("enable macro paradise to expand @tagless macro annotations")
class tagless extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro taglessImpl.tagless
}

trait TaglessEffectLike[F[_]] {
  self =>
  final type FS[A] = F[A]
}

/**
  * A tagless operation interpreted when a `H` Handler is provided
  */
trait Alg[H[_[_]], A] {
  self =>

  import cats.implicits._

  def apply[M[_] : Monad](implicit handler: H[M]): M[A]

  def exec[M[_] : Monad](implicit handler: H[M]): M[A] = apply[M]

  def map[B](f: A => B): Alg[H, B] = new Alg[H, B] {
    def apply[F[_] : Monad](implicit handler: H[F]): F[B] = self[F].map(f)
  }

  def flatMap[B](f: A => Alg[H, B]): Alg[H, B] =
    new Alg[H, B] {
      def apply[F[_] : Monad](implicit handler: H[F]): F[B] = self[F].flatMap(f(_)[F])
    }

}




