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

package freestyle.tagless.internal

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{Class, Object, Trait}
import freestyle.internal.ScalametaUtil._

trait TaglessEffectLike[F[_]] {
  final type FS[A] = F[A]
}

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object taglessImpl {
  import Util._

  def tagless(defn: Any): Stat = defn match {
    case cls: Trait =>
      freeAlg(Algebra(cls.mods, cls.name, cls.tparams, cls.ctor, cls.templ), isTrait = true)
    case cls: Class if isAbstract(cls) =>
      freeAlg(Algebra(cls.mods, cls.name, cls.tparams, cls.ctor, cls.templ), isTrait = false)
    case c: Class /* ! isAbstract */   => abort(s"$invalid in ${c.name}. $abstractOnly")
    case Term.Block(Seq(_, c: Object)) => abort(s"$invalid in ${c.name}. $noCompanion")
    case _                             => abort(s"$invalid. $abstractOnly")
  }

  def freeAlg(alg: Algebra, isTrait: Boolean): Term.Block =
    if (alg.requestDecls.isEmpty)
      abort(s"$invalid in ${alg.name}. $nonEmpty")
    else {
      val enriched = if (isTrait) alg.enrich.toTrait else alg.enrich.toClass
      Term.Block(Seq(enriched, alg.mkObject))
    }

}

case class Algebra(
    mods: Seq[Mod],
    name: Type.Name,
    tparams: Seq[Type.Param],
    ctor: Ctor.Primary,
    templ: Template
) {
  import Util._

  def toTrait: Trait = Trait(mods, name, tparams, ctor, templ)
  def toClass: Class = Class(mods, name, tparams, ctor, templ)

  val cleanedTParams: Seq[Type.Param] = tparams.toList match {
    case List(f@tparam"..$mods $name[$tparam]") => Nil
    case _ => tparams
  }

  val requestDecls: Seq[Decl.Def] = templ.stats.get.collect {
    case dd: Decl.Def =>
      dd.decltpe match {
        case Type.Apply(Type.Name("FS"), _) => dd
        case _                              => abort(s"$invalid in definition of method ${dd.name} in $name. $onlyReqs")
      }
  }

  // The enrich method adds a kind-1 type parameter `$ff[_]` to the algebra type,
  // making that trait extends from AnyRef.
  def enrich: Algebra = {
    val ff: Type.Name = Type.fresh("FF$")
    val pat           = tparams.toList match {
      case List(f @ tparam"..$mods $name[$tparam]") =>
        q"trait Foo[$f] extends _root_.freestyle.tagless.internal.TaglessEffectLike[${toType(f)}]"
      case _ =>
        val ff: Type.Name = Type.fresh("FF$")
        q"trait Foo[${tyParamK(ff)}] extends _root_.freestyle.tagless.internal.TaglessEffectLike[$ff]"
    }
    Algebra(mods, name, pat.tparams, ctor, templ.copy(parents = pat.templ.parents))
  }

  val requests: Seq[Request] = requestDecls.map(dd => new Request(dd))

  def mkObject: Object = {
    val mm = Type.fresh("MM$")
    val nn = Type.fresh("NN$")
    val hh = Term.fresh("hh$")

    val stackSafeHandler = Term.fresh("stackSafeHandler$")
    val functorK         = Pat.Var.Term.apply(Term.fresh("functorKInstance$"))

    val runTParams: Seq[Type.Param] = tyParamK(mm) +: cleanedTParams
    val runTArgs: Seq[Type]         = mm +: cleanedTParams.map(toType)

    val sup: Term.ApplyType = Term.ApplyType(Ctor.Ref.Name(name.value), runTArgs)

    val handlerT: Trait = q"""
      trait Handler[..$runTParams] extends $sup {
         ..${requests.map(_.handlerDef(mm))}
      }
    """

    val stackSafeT: Trait = q"""
      @_root_.freestyle.free 
      trait StackSafe {
        ..${requests.map(_.freeDef)}
      }
    """

    val stackSafeD: Defn.Def = {
      val ev = Term.fresh("ev$")
      q"""
      implicit def $stackSafeHandler[${tyParamK(mm)}](implicit $ev: _root_.cats.Monad[$mm], $hh: Handler[$mm]): StackSafe.Handler[$mm] =
        new StackSafe.Handler[$mm] {
          ..${requests.map(_.freeHandlerDef(hh, mm))}
        }
    """
    }

    val deriveDef: Defn.Def = {
      val deriveTTs = tyParamK(mm) +: tyParamK(nn) +: cleanedTParams
      val nnTTs     = nn +: cleanedTParams.map(toType)
      q"""
        implicit def derive[..$deriveTTs](
          implicit h: $name[..$runTArgs],
          FK: _root_.mainecoon.FunctorK[$name],
          fk: _root_.cats.arrow.FunctionK[$mm, $nn]
      ): $name[..$nnTTs] = FK.mapK(h)(fk)
      """
    }

    val applyDef: Defn.Def = {
      val ev = Term.fresh("ev$")
      q"def apply[..$runTParams](implicit $ev: $name[..$runTArgs]): $name[..$runTArgs] = $ev"
    }

    val functorKDef: Defn.Val = {
      val mapKTTs        = tyParamK(mm) +: tyParamK(nn) +: cleanedTParams
      val tParamsAsTypes = cleanedTParams.map(toType)
      val nnTTs          = nn +: tParamsAsTypes
      val ctor           = Ctor.Ref.Name(name.value)

      q"""
      implicit val $functorK: _root_.mainecoon.FunctorK[({ type λ[α[_]] = $name[α, ..$tParamsAsTypes] })#λ] =
        new _root_.mainecoon.FunctorK[({ type λ[α[_]] = $name[α, ..$tParamsAsTypes] })#λ] {
          def mapK[..$mapKTTs]($hh: $name[$mm, ..$tParamsAsTypes])(
            fk: _root_.cats.arrow.FunctionK[$mm, ..$nnTTs]): $name[..$nnTTs] =
              new $ctor[$nn, ..$tParamsAsTypes] {
                ..${requests.map(_.functorKDef(hh, nn))}
              }
        }"""
    }

    val prot = q"object X {}"
    prot.copy(
      name = Term.Name(name.value),
      templ = prot.templ.copy(
        stats = Some(Seq(applyDef, functorKDef, deriveDef, stackSafeT, stackSafeD, handlerT))
      ))
  }

}

class Request(reqDef: Decl.Def) {

  import reqDef.tparams

  // Name of the Request ADT Class
  private[this] val reqName: String = reqDef.name.value

  private[this] val res: Type = reqDef.decltpe match {
    case Type.Apply(_, args) => args.last
    case _                   => abort("Internal @tagless failure. Attempted to do request of non-applied type")
  }

  private[this] val reqImpl = Term.Name(reqName)

  val params: Seq[Term.Param] = reqDef.paramss.flatten

  def freeDef: Decl.Def =
    if (params.isEmpty)
      q"def $reqImpl[..$tparams]: FS[$res]"
    else
      q"def $reqImpl[..$tparams](..$params): FS[$res]"

  def freeHandlerDef(hH: Term.Name, rt: Type.Name): Defn.Def = {
    val args: Seq[Term.Name] = params.map(toName)
    if (params.isEmpty)
      q"def $reqImpl[..$tparams]: $rt[$res] = $hH.${reqDef.name}(..$args)"
    else
      q"def $reqImpl[..$tparams](..$params): $rt[$res] = $hH.${reqDef.name}(..$args)"
  }

  def functorKDef(hH: Term.Name, rt: Type.Name): Defn.Def = {
    val args: Seq[Term.Name] = params.map(toName)
    if (params.isEmpty)
      q"def $reqImpl[..$tparams]: $rt[$res] = fk($hH.${reqDef.name}(..$args))"
    else
      q"def $reqImpl[..$tparams](..$params): $rt[$res] = fk($hH.${reqDef.name}(..$args))"
  }

  def handlerDef(mm: Type.Name): Decl.Def =
    if (params.isEmpty)
      q"def $reqImpl[..$tparams]: $mm[$res]"
    else
      q"def $reqImpl[..$tparams](..$params): $mm[$res]"

}

object Util {
  // Messages of error
  val invalid = "Invalid use of the `@tagless` annotation"
  val abstractOnly =
    "The `@tagless` annotation can only be applied to a trait or to an abstract class."
  val noCompanion = "The trait (or class) annotated with `@tagless` must have no companion object."
  val onlyReqs =
    "In a `@tagless`-annotated trait (or class), all abstract method declarations should be of type FS[_]"
  val nonEmpty =
    "A `@tagless`-annotated trait or class  must have at least one abstract method of type `FS[_]`"
}
// $COVERAGE-ON$
