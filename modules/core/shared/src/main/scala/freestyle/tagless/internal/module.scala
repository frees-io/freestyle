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

package freestyle.tagless.internal

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{Class, Object, Trait}
import freestyle.free.internal._
import freestyle.free.internal.ScalametaUtil._

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object moduleImpl {

  val errors = new ErrorMessages("@module")
  import errors._

  def module(defn: Any): Term.Block = {
    val (clait, isTrait) = Clait.parse("@module", defn)
    val alg = TaglessModule(clait)
    val enriched = if (isTrait) alg.enrichClait.toTrait else alg.enrichClait.toClass
    val block = Term.Block(Seq(enriched, alg.makeObject))
    if (clait.mods.isDebug) println(block)
    block
  }

}

private[internal] case class TaglessModule( clait: Clait ) {
  val errors = new ErrorMessages("@module")
  import errors._
  import clait._

  val effects: Seq[ModEffect] = templ.stats.getOrElse(Nil).collect { case ModEffect(eff) => eff }

  def enrichStat(st: Stat): Stat = st match {
    case ModEffect(eff) =>
      eff.effVal.copy(decltpe = Type.Apply(eff.tyFunc, Seq(headTName)))
    case x => x
  }

  /* The effects are Val Declarations (no value definition) */
  def enrichClait: Clait = {
    val pat = q"trait Foo[$headTParam] extends _root_.freestyle.tagless.internal.TaglessEffectLike[${headTParam.toName}]"
    Clait(mods, name, Seq(headTParam), ctor, templ.copy(
      parents = pat.templ.parents,
      stats = templ.stats.map(_.map(enrichStat))
    ))
  }

  // The effects of a module are those variables declaration (not defined)
  // that are singular, i.e., not a tuple "val (x,y) = (1,2)"

  def lifterStats: (Class, Defn.Def) = {
    val gg: Type.Name              = Type.fresh("GG$")
    val toTParams: Seq[Type.Param] = gg.paramK +: tailTParams
    val toTArgs: Seq[Type]         = gg +: tailTNames

    val toDefParams: Seq[Term.Param] = effects.map(_.buildDefParam(gg))

    val toClass: Class = {
      val toClassParams: Seq[Term.Param] = toDefParams.map(_.addMod(Mod.ValParam()))
      val sup: Term.ApplyType = Term.ApplyType(Ctor.Ref.Name(name.value), toTArgs)
      q"class To[..$toTParams](implicit ..$toClassParams) extends $sup { }"
    }
    val toDef: Defn.Def =
      if (effects.isEmpty)
        q"implicit def to[..$toTParams]: To[..$toTArgs] = new To[..$toTArgs]"
      else
        q"implicit def to[..$toTParams](..$toDefParams): To[..$toTArgs] = new To[..$toTArgs]()"

    (toClass, toDef)
  }

  def makeObject: Object = {
    val (toClass, toDef) = lifterStats

    val prot = q"object X {}"
    prot.copy(
      name = Term.Name(name.value),
      templ = prot.templ.copy(
        stats = Some(Seq(toClass, toDef, clait.applyDef))
      ))
  }

}

private[internal] class ModEffect(val effVal: Decl.Val) {

  val name = effVal.pats.head.name

  val tyFunc: Type = effVal.decltpe match {
    case Type.Apply(tfun, _) => tfun
    case ty                  => ty
  }

  // buildParam(x, t) = q"def foo(implicit $x: $t[$gg])"
  def buildDefParam(gg: Type.Name): Term.Param =
    q"def foo(implicit x: Int)".paramss.head.head.copy(
      name = name,
      decltpe = Some( Type.Apply(tyFunc, Seq(gg)))
    )

  // from the reference to a type (a class), make the reference to its companion object
  def typeToObject(ty: Type): Term.Ref = ty match {
    case Type.Name(n)                 => Term.Name(n)
    case Type.Select(q, Type.Name(n)) => Term.Select(q, Term.Name(n))
    case Type.Apply(t, Seq(_))        => typeToObject(t)
    case _                            => abort(s"found: $ty unmatched. What is the case here")
  }

}

object ModEffect {

  def unapply(stat: Stat): Option[ModEffect] = stat match {
    case vdec @ Decl.Val(_, Seq(Pat.Var.Term(_)), _ ) => Some(new ModEffect(vdec) )
    case _ => None
  }

}

// $COVERAGE-ON$
