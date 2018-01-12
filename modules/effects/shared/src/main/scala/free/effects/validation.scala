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

object validation {

  final class ValidationProvider[E] {

    val taglessV: freestyle.tagless.effects.validation.ValidationProvider[E] =
      freestyle.tagless.effects.validation[E]

    type ValidationM[F[_]] = taglessV.ValidationM.StackSafe[F]

    val ValidationM = taglessV.ValidationM.StackSafe

    trait FreeImplicits extends taglessV.Implicits {

      implicit class FreeValidSyntax[A](private val s: A) {
        def liftValid[F[_]: ValidationM] = ValidationM[F].valid(s)
      }
      implicit class FreeInvalidSyntax[A](private val e: E) {
        def liftInvalid[F[_]: ValidationM] = ValidationM[F].invalid(e)
      }

    }

    object implicits extends FreeImplicits

  }

  def apply[E] = new ValidationProvider[E]

}
