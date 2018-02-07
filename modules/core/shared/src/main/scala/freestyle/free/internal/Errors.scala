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

package freestyle.free.internal

class ErrorMessages(annotation: String){

  // Messages of error
  val invalid = s"Invalid use of `$annotation`"
  val abstractOnly = s"The `$annotation` annotation can only be applied to a trait or an abstract class."
  val noCompanion = "The trait or class annotated with `$annotation` must have no companion object."

  val onlyReqs =
    s"In a `$annotation`-annotated trait (or class), all abstract method declarations should be of type FS[_]"
  val nonEmpty =
    s"A `$annotation`-annotated trait or class  must have at least one abstract method of type `FS[_]`"

}

