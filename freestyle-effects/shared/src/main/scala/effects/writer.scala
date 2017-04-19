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

import cats.MonadWriter

object writer {

  final class AccumulatorProvider[W] {

    @free sealed abstract class WriterM {
      def writer[A](aw: (W, A)): OpPar[A]
      def tell(w: W): OpPar[Unit]
    }

    object implicits {

      implicit def freestyleWriterMHandler[M[_]](
          implicit MW: MonadWriter[M, W]): WriterM.Handler[M] = new WriterM.Handler[M] {
        def writer[A](aw: (W, A)): M[A] = MW.writer(aw)
        def tell(w: W): M[Unit]         = MW.tell(w)
      }

    }

  }

  def apply[W] = new AccumulatorProvider[W]

}
