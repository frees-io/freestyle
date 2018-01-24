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

package freestyle
package tagless

import freestyle.tagless.internal._

import scala.annotation.{StaticAnnotation, compileTimeOnly}

@compileTimeOnly("enable macro paradise to expand @tagless macro annotations")
class tagless extends StaticAnnotation {
  import scala.meta._

  inline def apply(defn: Any): Any = meta { taglessImpl.tagless(defn, false) }
}

@compileTimeOnly("enable macro paradise to expand @tagless macro annotations")
class taglessStackSafe extends StaticAnnotation {
  import scala.meta._

  inline def apply(defn: Any): Any = meta { taglessImpl.tagless(defn, true) }
}

@compileTimeOnly("enable macro paradise to expand @module macro annotations")
class module extends StaticAnnotation {
  import scala.meta._

  inline def apply(defn: Any): Any = meta { moduleImpl.module(defn) }
}