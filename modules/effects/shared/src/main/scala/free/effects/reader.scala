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
package effects

object reader {

  final class EnvironmentProvider[R] {

    val taglessV: freestyle.tagless.effects.reader.EnvironmentProvider[R] =
      freestyle.tagless.effects.reader[R]

    type ReaderM[F[_]] = taglessV.ReaderM.StackSafe[F]

    val ReaderM = taglessV.ReaderM.StackSafe

    object implicits extends taglessV.Implicits

  }

  def apply[R] = new EnvironmentProvider[R]

}
