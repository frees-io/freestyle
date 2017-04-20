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

/** This tests reproduces the bug in https://github.com/47deg/freestyle/issues/165
  *
  * The problem was that the "@module" macro was not using _root_ to start the type 
  * references, which causes some collisions. This is a problem of macro hygiene.
  */

package issue165

import freestyle._

// The root of this package is freestyle.issue165.issue165
object issue165

@free trait India {
  def kolkata: FS[Int]
}

@module trait Asia[F[_]] {
  val india: India[F]
}
