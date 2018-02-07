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

import freestyle.free.FreeS
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{Class, Object, Trait}

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object moduleImpl {

  val errors = new ErrorMessages("@module")
  import errors._
  import ModuleUtil._
  import syntax._

  def module(defn: Any): Term.Block = defn match {
    case cls: Trait =>
      val fsmod = FreeSModule(Clait(cls))
      Term
        .Block(Seq(fsmod.enrichClait.toTrait, fsmod.makeObject))
        .`debug?`(cls.mods)
    case cls: Class if ScalametaUtil.isAbstract(cls) =>
      val fsmod = FreeSModule(Clait(cls))
      Term
        .Block(Seq(fsmod.enrichClait.toClass, fsmod.makeObject))
        .`debug?`(cls.mods)
    case c: Class /* ! isAbstract */ =>
      abort(abstractOnly)
    case Term.Block(Seq(_, c: Object)) =>
      abort(noCompanion)
    case _ =>
      abort("Unexpected trees $trees encountered for `@module` annotation")
  }

}

private[internal] case class FreeSModule( clait: Clait ) {
  import ModuleUtil._
  val errors = new ErrorMessages("@module")
  import errors._
  import ScalametaUtil._
  import clait._

  val effects: Seq[ModEffect] =
    templ.stats.getOrElse(Nil).collect {
      case vdec @ Decl.Val(_, Seq(Pat.Var.Term(_)), Type.Apply(tname @ _, Seq(Type.Name(_)))) =>
        ModEffect(vdec)
      case vdec @ Decl.Val(_, Seq(Pat.Var.Term(_)), _) => ModEffect(vdec)
    }

  def enrichStat(st: Stat): Stat = st match {
    case vdec @ Decl.Val(_, Seq(Pat.Var.Term(tname)), Type.Apply(_, Seq(_))) =>
      vdec
    case vdec @ Decl.Val(_, Seq(Pat.Var.Term(tname)), ty) =>
      vdec.copy(decltpe = Type.Apply(ty, Seq(headTParam.toName)))
    case x => x
  }

  /* The effects are Val Declarations (no value definition) */
  def enrichClait: Clait = {
    val ff = headTParam
    val pat = q"trait Foo[$ff] extends _root_.freestyle.free.internal.EffectLike[${ff.toName}]"
    Clait(mods, name, pat.tparams, ctor, templ.copy(
      parents = pat.templ.parents,
      stats = templ.stats.map(_.map(enrichStat))
    ))
  }

  // The effects of a module are those variables declaration (not defined)
  // that are singular, i.e., not a tuple "val (x,y) = (1,2)"

  val applyDefConcreteOp: Defn.Def =
    q"def instance(implicit ev: $name[Op]): $name[Op] = ev"

  def lifterStats: (Class, Defn.Def) = {
    val gg: Type.Name              = Type.fresh("GG$")
    val toTParams: Seq[Type.Param] = gg.paramK +: tailTParams
    val toTArgs: Seq[Type]         = gg +: tailTNames

    val toClass: Class = {
      val sup: Term.ApplyType = Term.ApplyType(name.ctor, toTArgs)
      val args: Seq[Term.Param] = effects.map(_.buildConstParam(gg))
      q"class To[..$toTParams](implicit ..$args) extends $sup { }"
    }

    val toDef: Defn.Def =
      if (effects.isEmpty)
        q"implicit def to[..$toTParams]: To[..$toTArgs] = new To[..$toTArgs]"
      else {
        val args: Seq[Term.Param] = effects.map(_.buildDefParam(gg))
        q"implicit def to[..$toTParams](..$args): To[..$toTArgs] = new To[..$toTArgs]()"
      }

    (toClass, toDef)
  }

  def makeObject: Object = {
    val (toClass, toDef) = lifterStats
    val opType: Defn.Type = {
      val aa: Type.Name = Type.fresh("AA$")
      q"type Op[${aa.param}] = _root_.iota.CopK[OpTypes, $aa]"
    }
    val opTypes = q"type OpTypes = T".copy(body = makeOpTypesBody)

    val prot = q"object X {}"
    prot.copy(
      name = Term.Name(name.value),
      templ = prot.templ.copy(
        stats = Some(Seq(opTypes, opType, toClass, toDef, clait.applyDef, applyDefConcreteOp))
      ))
  }

  def makeOpTypesBody: Type =
    if (effects.isEmpty)
      iotaTNilKT
    else
      effects.map(_.opType).reduce(iotaConcatApply)

}

private[internal] case class ModEffect(effVal: Decl.Val) {

  // buildParam(x, t) = q"def foo(implicit $x: $t[$gg])"
  def buildDefParam(gg: Type.Name): Term.Param =
    q"def foo(implicit x: Int)".paramss.head.head.copy(
      name = effVal.pats.head.name,
      decltpe = Some(effVal.decltpe match {
        case Type.Apply(t @ _, _) => Type.Apply(t, Seq(gg))
        case _                    => Type.Apply(effVal.decltpe, Seq(gg))
      })
    )

  // buildParam(x, t) = q"class Foo(implicit val $x: $t[$gg])"
  def buildConstParam(gg: Type.Name): Term.Param =
    q"def foo(implicit x: Int)".paramss.head.head.copy(
      mods = Seq(Mod.Implicit(), Mod.ValParam()),
      name = effVal.pats.head.name,
      decltpe = Some(effVal.decltpe match {
        case Type.Apply(t @ _, _) => Type.Apply(t, Seq(gg))
        case _                    => Type.Apply(effVal.decltpe, Seq(gg))
      })
    )

  // x => q"x.OpTypes"
  val opType: Type.Select = Type.Select(typeToObject(effVal.decltpe), Type.Name("OpTypes"))

  // from the reference to a type (a class), make the reference to its companion object
  def typeToObject(ty: Type): Term.Ref = ty match {
    case Type.Name(n)                 => Term.Name(n)
    case Type.Select(q, Type.Name(n)) => Term.Select(q, Term.Name(n))
    case Type.Apply(t, Seq(_))        => typeToObject(t)
    case _                            => abort(s"found: $ty unmatched. What is the case here")
  }

}

private[internal] object ModuleUtil {
  // Messages of error

  val iotaTNilKT: Type  = q"type T = _root_.iota.TNilK".body
  val iotaConcatT: Type = q"type T = _root_.iota.TListK.Op.Concat".body

  def iotaConcatApply(ta: Type, tb: Type): Type.Apply = Type.Apply(iotaConcatT, Seq(ta, tb))
}
// $COVERAGE-ON$
