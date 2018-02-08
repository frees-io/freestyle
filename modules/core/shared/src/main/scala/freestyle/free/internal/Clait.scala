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

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{Class, Trait, Object}

/* A Clait in this context generalises the shared elements of a Class or a Trait
 */
case class Clait(
  mods: Seq[Mod],
  name: Type.Name,
  tparams: Seq[Type.Param],
  ctor: Ctor.Primary,
  templ: Template
) {

  import ScalametaUtil._
  import syntax._

  def toTrait: Trait = Trait(mods.filtered, name, tparams, ctor, templ)
  def toClass: Class = Class(mods.filtered, name, tparams, ctor, templ)

  val allTParams: Seq[Type.Param] = tparams.toList match {
    case headParam :: tail if headParam.isKind1 => tparams
    case _ => Type.fresh("FF$").paramK :: tparams.toList
  }
  val allTNames: Seq[Type.Name] = allTParams.map(_.toName)
  val headTParam: Type.Param = allTParams.head
  val headTName: Type.Name = allTNames.head
  val tailTParams: Seq[Type.Param] = allTParams.tail
  val tailTNames: Seq[Type.Name] = tailTParams.map(_.toName)


  def applyDef: Defn.Def = {
    val ev = Term.fresh("ev$")
    q"def apply[..$allTParams](implicit $ev: $name[..$allTNames]): $name[..$allTNames] = $ev"
  }

}

object Clait {

  def apply(cls: Class): Clait = Clait(cls.mods, cls.name, cls.tparams, cls.ctor, cls.templ)
  def apply(cls: Trait): Clait = Clait(cls.mods, cls.name, cls.tparams, cls.ctor, cls.templ)

  def parse(annotation: String, defn: Any): (Clait, Boolean) = {
    val errors = new ErrorMessages(annotation)
    import errors._
    import ScalametaUtil.isAbstract

    defn match {
      case cls: Trait => (Clait(cls), true)
      case cls: Class if isAbstract(cls) => (Clait(cls), false)
      case c: Class /* ! isAbstract */   => abort(s"$invalid in ${c.name}. $abstractOnly")
      case Term.Block(Seq(_, c: Object)) => abort(s"$invalid in ${c.name}. $noCompanion")
      case _                             => abort(s"$unexpected. $abstractOnly")
    }
  }

}
