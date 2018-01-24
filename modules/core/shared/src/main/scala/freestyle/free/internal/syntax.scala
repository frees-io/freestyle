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
package free.internal

import scala.collection.immutable.Seq
import scala.meta._

object syntax {

  implicit def debugSyntax(block: Term.Block): DebugOps = new DebugOps(block)

  implicit def filterModifiers(mods: Seq[Mod]): ModOps = new ModOps(mods)

  final class DebugOps(block: Term.Block) {

    def `debug?`(mods: Seq[Mod]): Term.Block = {
      mods foreach {
        case mod"@debug" => println(block)
        case _           =>
      }
      block
    }
  }

  final class ModOps(mods: Seq[Mod]) {
    def filtered: Seq[Mod] = mods.filter {
      case mod"@debug" => false
      case mod"@stacksafe" => false
      case _           => true
    }

    def isStackSafe: Boolean = mods.exists {
      case mod"@stacksafe" => true
      case _ => false
    }
  }

}
