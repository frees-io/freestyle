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

import freestyle.free.internal._
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{Class, Object, Trait}

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object moduleImpl {

  val errors = new ErrorMessages("@module")
  import errors._
  import syntax._
  import freestyle.free.internal.ScalametaUtil._

  def module(defn: Any): Term.Block = {
    val (clait, isTrait) = Clait.parse("@module", defn)
    val alg = TaglessModule(clait)
    val enriched = if (isTrait) alg.enrichClait.toTrait else alg.enrichClait.toClass
    Term.Block(Seq(enriched, alg.makeObject)).`debug?`(clait.mods)
  }

}

private[internal] case class TaglessModule( clait: Clait ) {
  val errors = new ErrorMessages("@module")
  import errors._
  import ScalametaUtil._
  import clait._

  val effects: Seq[ModEffect] =
    templ.stats.getOrElse(Nil).collect {
      case vdec @ Decl.Val(_, Seq(Pat.Var.Term(_)), Type.Apply(tname @ _, Seq(Type.Name(_)))) => ModEffect(vdec)
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
    val pat = q"trait Foo[$ff] extends _root_.freestyle.tagless.internal.TaglessEffectLike[${ff.toName}]"
    Clait(mods, name, pat.tparams, ctor, templ.copy(
      parents = pat.templ.parents,
      stats = templ.stats.map(_.map(enrichStat))
    ))
  }

  // The effects of a module are those variables declaration (not defined)
  // that are singular, i.e., not a tuple "val (x,y) = (1,2)"

  def lifterStats: (Class, Defn.Def, Defn.Def) = {
    val gg: Type.Name              = Type.fresh("GG$")
    val toTParams: Seq[Type.Param] = gg.paramK +: tailTParams
    val toTArgs: Seq[Type]         = gg +: tailTNames

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

    (toClass, toDef, clait.applyDef)
  }

  def makeObject: Object = {
    val (toClass, toDef, applyDef) = lifterStats

    val prot = q"object X {}"
    prot.copy(
      name = Term.Name(name.value),
      templ = prot.templ.copy(
        stats = Some(Seq(toClass, toDef, applyDef))
      ))
  }

}

private[internal] case class ModEffect(effVal: Decl.Val) {

  // buildParam(x, t) = q"def foo(implicit $x: $t[$gg])"
  def buildDefParam(gg: Type.Name): Term.Param =
    q"def foo(implicit x: Int)".paramss.head.head.copy(
      name = effVal.pats.head.name,
      decltpe = Some(effVal.decltpe match {
        case Type.Apply(t @ _, _) => Type.Apply(t, Seq(gg))
        case _ => Type.Apply(effVal.decltpe, Seq(gg))
      })
    )

  // buildParam(x, t) = q"class Foo(implicit val $x: $t[$gg])"
  def buildConstParam(gg: Type.Name): Term.Param =
    q"def foo(implicit x: Int)".paramss.head.head.copy(
      mods = Seq(Mod.Implicit(), Mod.ValParam()),
      name = effVal.pats.head.name,
      decltpe = Some(effVal.decltpe match {
        case Type.Apply(t @ _, _) => Type.Apply(t, Seq(gg))
        case _ => Type.Apply(effVal.decltpe, Seq(gg))
      })
    )

  // from the reference to a type (a class), make the reference to its companion object
  def typeToObject(ty: Type): Term.Ref = ty match {
    case Type.Name(n)                 => Term.Name(n)
    case Type.Select(q, Type.Name(n)) => Term.Select(q, Term.Name(n))
    case Type.Apply(t, Seq(_))        => typeToObject(t)
    case _                            => abort(s"found: $ty unmatched. What is the case here")
  }

}

// $COVERAGE-ON$
