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
import freestyle.free.internal.ScalametaUtil._
import freestyle.free.internal.{Algebra => FreeAlgebra}

trait TaglessEffectLike[F[_]] {
  final type FS[A] = F[A]
}

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object taglessImpl {
  import Util._

  import freestyle.free.internal.syntax._

  def tagless(defn: Any, isStackSafe: Boolean): Stat = defn match {
    case cls: Trait =>
      freeAlg(Algebra(cls.mods.filtered, cls.name, cls.tparams, cls.ctor, cls.templ), isTrait = true)
        .`debug?`(cls.mods)
    case cls: Class if isAbstract(cls) =>
      freeAlg(Algebra(cls.mods.filtered, cls.name, cls.tparams, cls.ctor, cls.templ), isTrait = false, isStackSafe = isStackSafe)
        .`debug?`(cls.mods)

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
    templ: Template,
    isStackSafe: Boolean
) {
  import Util._

  def toTrait: Trait = Trait(mods, name, tparams, ctor, templ)
  def toClass: Class = Class(mods, name, tparams, ctor, templ)

  def pickOrGenerateFF( tparams: Seq[Type.Param]): List[Type.Param] = {
    def isKind1(tparam: Type.Param): Boolean =
      tparam.tparams.toList match {
        case List(tp) if tp.tparams.isEmpty => true
        case _ => false
      }
    tparams.toList match {
      case headParam :: tail if isKind1(headParam) => headParam :: tail
      case _ => Type.fresh("FF$").paramK :: tparams.toList
    }
  }

  val allTParams: Seq[Type.Param] = pickOrGenerateFF(tparams)
  val allTNames: Seq[Type.Name] = allTParams.map(_.toName)
  val fs: Type.Param = allTParams.head
  val tailTParams: Seq[Type.Param] = allTParams.tail
  val tailTNames: Seq[Type.Name] = tailTParams.map(_.toName)

  val cleanedTParams = tailTParams

  val requestDecls: Seq[Decl.Def] = templ.stats.get.collect {
    case dd: Decl.Def =>
      dd.decltpe match {
        case Type.Apply(Type.Name("FS"), _) => dd
        case Type.Apply(Type.Name(ff), _) if ff == fs.toName.value => dd
        case _                              => abort(s"$invalid in definition of method ${dd.name} in $name. $onlyReqs")
      }
  }
  val requests: Seq[Request] = requestDecls.map(dd => new Request(dd))

  // The enrich method adds a kind-1 type parameter `$ff[_]` to the algebra type,
  // making that trait extends from AnyRef.
  def enrich: Algebra = {
    val pat = q"trait Foo[$fs] extends _root_.freestyle.tagless.internal.TaglessEffectLike[${fs.toName}]"
    val self = Term.fresh("self$")
    Algebra(mods, name, allTParams, ctor, templ.copy(
      parents = pat.templ.parents,
      self = self.param,
      stats = templ.stats.map( _ :+ mapKDef(self))
    ))
  }

  def mapKDef(sf: Term.Name): Defn.Def = {
    val mm = Type.fresh("MM$")
    val fk = Term.fresh("fk$")

    q"""
      def mapK[${mm.paramK}](
        $fk: _root_.cats.arrow.FunctionK[${fs.toName}, $mm]
      ): $name[$mm, ..$tailTNames] =
        new ${name.ctor}[$mm, ..$tailTNames] {
          ..${requests.map(_.mapKDef(fk, sf, mm))}
        }
    """
  }

  def mkObject: Object = {
    val mm = Type.fresh("MM$")
    val nn = Type.fresh("NN$")
    val runTParams: Seq[Type.Param] = tyParamK(mm) +: cleanedTParams
    val runTArgs: Seq[Type]         = mm +: cleanedTParams.map(toType)

    val handlerT: Trait =
      if (isStackSafe)
        q"""
          trait Handler[..$allTParams] extends ${name.ctor}[..$allTNames] with StackSafe.Handler[..$allTNames] {
            ..${requestDecls.map(_.addMod(Mod.Override()))}
          }
        """
      else 
        q"""
          trait Handler[..$allTParams] extends ${name.ctor}[..$allTNames] {
            ..${requestDecls.map(_.addMod(Mod.Override()))}
          }
        """

    lazy val stackSafeAlg: FreeAlgebra = {
      def withFS( req: Decl.Def): Decl.Def =
        req.copy(decltpe = req.decltpe match {
          case Type.Apply(_, targs) => Type.Apply( Type.Name("FS"), targs)
          case _ => req.decltpe
        })

      val t: Trait = q" trait StackSafe { ..${requestDecls.map(withFS)} } "
      FreeAlgebra(Seq.empty[Mod], Type.Name("StackSafe"), allTParams, t.ctor, t.templ)
    }
    lazy val stackSafeT: Trait = stackSafeAlg.enrich.toTrait
    lazy val stackSafeD: Object = stackSafeAlg.mkCompanion

    val deriveDef: Defn.Def = {
      val deriveTTs = tyParamK(mm) +: tyParamK(nn) +: cleanedTParams
      val nnTTs     = nn +: cleanedTParams.map(toType)
      q"""
        implicit def derive[..$deriveTTs](
          implicit h: $name[..$runTArgs],
          fk: _root_.cats.arrow.FunctionK[$mm, $nn]
      ): $name[..$nnTTs] = h.mapK[$nn](fk)
      """
    }

    val applyDef: Defn.Def = {
      val ev = Term.fresh("ev$")
      q"def apply[..$runTParams](implicit $ev: $name[..$runTArgs]): $name[..$runTArgs] = $ev"
    }

    val functorKDef: Defn.Val = {
      val functorK       = Pat.Var.Term.apply(Term.fresh("functorKInstance$"))
      val mapKTTs        = tyParamK(mm) +: tyParamK(nn) +: cleanedTParams
      val tParamsAsTypes = cleanedTParams.map(toType)
      val hh = Term.fresh("hh$")

      q"""
      implicit val $functorK: _root_.mainecoon.FunctorK[({ type λ[α[_]] = $name[α, ..$tParamsAsTypes] })#λ] =
        new _root_.mainecoon.FunctorK[({ type λ[α[_]] = $name[α, ..$tParamsAsTypes] })#λ] {
          def mapK[..$mapKTTs]($hh: $name[$mm, ..$tParamsAsTypes])(
            fk: _root_.cats.arrow.FunctionK[$mm, $nn]): $name[$nn, ..$tailTNames] =
              $hh.mapK(fk)
        }"""
    }

    val prot = q"object X {}"
    prot.copy(
      name = Term.Name(name.value),
      templ = prot.templ.copy(
        stats = Some(
          if (isStackSafe)
            Seq(applyDef, functorKDef, deriveDef, stackSafeT, stackSafeD, handlerT)
          else Seq(applyDef, functorKDef, deriveDef, handlerT)
        )
      ))
  }

}

private[freestyle] class Request(reqDef: Decl.Def) {

  private[this] val res: Type = reqDef.decltpe match {
    case Type.Apply(_, args) => args.last
    case _                   => abort("Internal @tagless failure. Attempted to do request of non-applied type")
  }

  def mapKDef(fk: Term.Name, hh: Term.Name, rt: Type.Name): Defn.Def =
    reqDef
      .withType(Type.Apply( rt, Seq(res)))
      .addBody( q"$fk($hh.${reqDef.name}(...${reqDef.argss}))" )

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
