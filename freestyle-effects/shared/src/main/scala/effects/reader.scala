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
package effects

import cats.MonadReader

object reader {

  final class EnvironmentProvider[R] {

    @free abstract class ReaderM {
      def ask: OpPar[R]
      def reader[B](f: R => B): OpPar[B]
    }

    object implicits {

      implicit def freestyleReaderMHandler[M[_]](implicit MR: MonadReader[M, R]): ReaderM.Handler[M] =
        new ReaderM.Handler[M] {
          def ask: M[R]                  = MR.ask
          def reader[B](f: R => B): M[B] = MR.reader(f)
        }
    }

  }

  def apply[R] = new EnvironmentProvider[R]

}
