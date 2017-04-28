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

/** Implicit options to configure/control Freestyle's macros
  */
package object debug {
  object optionTypes {
    sealed trait ShowTrees
  }
  import optionTypes._

  object options {

    /** Import this value to have Freestyle print the macro generated code
      * to the console during compilation
      */
    implicit val ShowTrees: ShowTrees = null.asInstanceOf[ShowTrees]
  }
}
