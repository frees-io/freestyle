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

package freestyle.tagless

import scala.concurrent._
import scala.util._
import freestyle.async._
import freestyle.async.implicits._

object async {

  /** Async computation algebra. **/
  @tagless trait AsyncM {
    def async[A](fa: Proc[A]): FS[A]
  }

  trait Implicits {

    implicit def taglessAsyncMHandler[M[_]](implicit MA: AsyncContext[M]): AsyncM.Handler[M] =
      new AsyncM.Handler[M] {
        def async[A](fa: Proc[A]): M[A] = MA.runAsync(fa)
      }
  }

  object implicits extends Implicits
}
