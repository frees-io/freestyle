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

package freestyle.internal

import freestyle.FreeS
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{Class, Object, Trait}

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object moduleImpl {

  import ModuleUtil._
  import syntax._

  def module(defn: Any): Term.Block = defn match {
    case cls: Trait =>
      val fsmod =
        FreeSModule(cls.mods.filtered, cls.name, cls.tparams, cls.ctor, cls.templ, isTrait = true)
      Term
        .Block(Seq(fsmod.makeClass, fsmod.makeObject))
        .`debug?`(cls.mods)
    case cls: Class if ScalametaUtil.isAbstract(cls) =>
      val fsmod =
        FreeSModule(cls.mods.filtered, cls.name, cls.tparams, cls.ctor, cls.templ, isTrait = false)
      Term
        .Block(Seq(fsmod.makeClass, fsmod.makeObject))
        .`debug?`(cls.mods)
    case c: Class /* ! isAbstract */ =>
      abort(abstractOnly)
    case Term.Block(Seq(_, c: Object)) =>
      abort(noCompanion)
    case _ =>
      abort("Unexpected trees $trees encountered for `@module` annotation")
  }

}

private[internal] case class FreeSModule(
    mods: Seq[Mod],
    name: Type.Name,
    tparams: Seq[Type.Param],
    ctor: Ctor.Primary,
    templ: Template,
    isTrait: Boolean
) {
  import ModuleUtil._
  import ScalametaUtil._

  val effects: Seq[ModEffect] =
    templ.stats.getOrElse(Nil).collect {
      case vdec @ Decl.Val(_, Seq(Pat.Var.Term(_)), _) => ModEffect(vdec)
    }

  def enrichStat(tt: Type.Name, st: Stat): Stat = st match {
    case vdec @ Decl.Val(mods, Seq(Pat.Var.Term(tname)), ty) =>
      vdec.copy(decltpe = Type.Apply(ty, Seq(tt)))
    case x => x
  }

  /* The effects are Val Declarations (no value definition) */
  def makeClass: Defn = {
    val ff: Type.Name = Type.fresh("FF$")
    val pat           = q"trait Foo[${tyParamK(ff)}] extends _root_.freestyle.internal.EffectLike[$ff]"

    val nstats = templ.stats.map(_.map(stat => enrichStat(ff, stat)))
    val ntempl = templ.copy(parents = pat.templ.parents, stats = nstats)

    if (isTrait)
      Trait(mods, name, pat.tparams, ctor, ntempl)
    else
      Class(mods, name, pat.tparams, ctor, ntempl)
  }

  // The effects of a module are those variables declaration (not defined)
  // that are singular, i.e., not a tuple "val (x,y) = (1,2)"

  def lifterStats: (Class, Defn.Def, Defn.Def) = {
    val gg: Type.Name              = Type.fresh("GG$")
    val toTParams: Seq[Type.Param] = tyParamK(gg) +: tparams
    val toTArgs: Seq[Type]         = gg +: tparams.map(toType)

    val sup: Term.ApplyType = Term.ApplyType(Ctor.Ref.Name(name.value), toTArgs)
    val toClass: Class = {
      val args: Seq[Term.Param] = effects.map(_.buildConstParam(gg))
      q"class To[..$toTParams](implicit ..$args) extends $sup { }"
    }
    val toDef: Defn.Def = {
      val args: Seq[Term.Param] = effects.map(_.buildDefParam(gg))
      if (args.isEmpty)
        q"implicit def to[..$toTParams]: To[..$toTArgs] = new To[..$toTArgs]"
      else
        q"implicit def to[..$toTParams](..$args): To[..$toTArgs] = new To[..$toTArgs]()"
    }
    val applyDef: Defn.Def =
      q"def apply[..$toTParams](implicit ev: $name[..$toTArgs]): $name[..$toTArgs] = ev"
    (toClass, toDef, applyDef)
  }

  def makeObject: Object = {
    val (toClass, toDef, applyDef) = lifterStats
    val opType: Defn.Type = {
      val aa: Type.Name = Type.fresh("AA$")
      q"type Op[${tyParam(aa)}] = _root_.iota.CopK[OpTypes, $aa]"
    }
    val opTypes = q"type OpTypes = T".copy(body = makeOpTypesBody)

    val prot = q"object X {}"
    prot.copy(
      name = Term.Name(name.value),
      templ = prot.templ.copy(
        stats = Some(Seq(opTypes, opType, toClass, toDef, applyDef))
      ))
  }

  def makeOpTypesBody: Type =
    if (effects.isEmpty)
      iotaKNilT
    else
      effects.map(_.opType).reduce(iotaConcatApply)

}

private[internal] case class ModEffect(effVal: Decl.Val) {

  // buildParam(x, t) = q"def foo(implicit $x: $t[$gg])"
  def buildDefParam(gg: Type.Name): Term.Param =
    q"def foo(implicit x: Int)".paramss.head.head.copy(
      name = effVal.pats.head.name,
      decltpe = Some(Type.Apply(effVal.decltpe, Seq(gg)))
    )

  // buildParam(x, t) = q"class Foo(implicit val $x: $t[$gg])"
  def buildConstParam(gg: Type.Name): Term.Param =
    q"def foo(implicit x: Int)".paramss.head.head.copy(
      mods = Seq(Mod.Implicit(), Mod.ValParam()),
      name = effVal.pats.head.name,
      decltpe = Some(Type.Apply(effVal.decltpe, Seq(gg)))
    )

  // x => q"x.OpTypes"
  val opType: Type.Select = Type.Select(typeToObject(effVal.decltpe), Type.Name("OpTypes"))

  // from the reference to a type (a class), make the reference to its companion object
  def typeToObject(ty: Type): Term.Ref = ty match {
    case Type.Name(n)                 => Term.Name(n)
    case Type.Select(q, Type.Name(n)) => Term.Select(q, Term.Name(n))
    case _                            => abort("What is the case here")
  }

}

private[internal] object ModuleUtil {
  // Messages of error
  val invalid = "Invalid use of `@module`"
  val abstractOnly =
    "The `@module` annotation can only be applied to a trait or an abstract class."
  val noCompanion = "The trait or class annotated with `@module` must have no companion object."

  val iotaKNilT: Type   = q"type T = _root_.iota.KNil".body
  val iotaConcatT: Type = q"type T = _root_.iota.KList.Op.Concat".body

  def iotaConcatApply(ta: Type, tb: Type): Type.Apply = Type.Apply(iotaConcatT, Seq(ta, tb))
}
// $COVERAGE-ON$
