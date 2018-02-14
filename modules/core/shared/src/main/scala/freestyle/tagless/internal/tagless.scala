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
import freestyle.free.internal.{Algebra => FreeAlgebra, Clait, ErrorMessages}

trait TaglessEffectLike[F[_]] {
  final type FS[A] = F[A]
}

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object taglessImpl {
  val errors = new ErrorMessages("@tagless")
  import errors._

  def tagless(defn: Any, stackSafe: Boolean): Stat = {
    val (clait, isTrait) = Clait.parse("@tagless", defn)
    val alg = Algebra(clait, stackSafe)
    if (alg.requestDecls.isEmpty)
      abort(s"$invalid in ${alg.clait.name}. $nonEmpty")
    else {
      val enriched = if (isTrait) alg.enrich.toTrait else alg.enrich.toClass
      val block = Term.Block(Seq(enriched, alg.mkObject))
      if (clait.mods.isDebug) println(block)
      block
    }
  }

}

case class Algebra( clait: Clait, isStackSafe: Boolean) {
  import clait._
  val errors = new ErrorMessages("@tagless")
  import errors._

  val requestDecls: Seq[Decl.Def] = templ.stats.get.collect {
    case dd: Decl.Def =>
      dd.decltpe match {
        case Type.Apply(Type.Name("FS"), _) => dd
        case Type.Apply(Type.Name(ff), _) if ff == headTParam.toName.value => dd
        case _                              => abort(s"$invalid in definition of method ${dd.name} in $name. $onlyReqs")
      }
  }
  val requests: Seq[Request] = requestDecls.map(dd => new Request(dd))

  // The enrich method adds a kind-1 type parameter `$ff[_]` to the algebra type,
  // making that trait extends from AnyRef.
  def enrich: Clait = {
    val ff: Type.Param = headTParam
    val pat = q"trait Foo[$ff] extends _root_.freestyle.tagless.internal.TaglessEffectLike[${ff.toName}]"
    val self = Term.fresh("self$")
    Clait(mods, name, allTParams, ctor, templ.copy(
      parents = pat.templ.parents,
      self = self.param,
      stats = templ.stats.map( _ :+ mapKDef(name, self))
    ))
  }

  def mapKDef(tyName: Type.Name, sf: Term.Name): Defn.Def = {
    val mm = Type.fresh("MM$")
    val fk = Term.fresh("fk$")
    q"""
      def mapK[${mm.paramK}](
        $fk: _root_.cats.arrow.FunctionK[${headTParam.toName}, $mm]
      ): $tyName[$mm, ..$tailTNames] =
        new ${tyName.ctor}[$mm, ..$tailTNames] {
          ..${requests.map(_.mapKDef(fk, sf, mm))}
        }
    """
  }

  def mkObject: Object = {
    val mm = Type.fresh("MM$")
    val nn = Type.fresh("NN$")

    val handlerT: Trait = {
      val sf = Term.fresh("self$")
      val handlerMapk: Defn.Def = mapKDef( Type.Name("Handler"), sf).addMod(Mod.Override())

      val tr = q"""
        trait Handler[..$allTParams] extends ${name.ctor}[..$allTNames] { ${sf.param} =>
          ..${requestDecls.map(_.addMod(Mod.Override()))}
          $handlerMapk
        }
      """
      if (isStackSafe) {
        val par = q"trait X extends StackSafe.Handler[..$allTNames]".templ.parents.last
        tr.copy( templ = tr.templ.addParent(par))
      } else tr
    }

    lazy val stackSafeAlg: FreeAlgebra = {
      def withFS( req: Decl.Def): Decl.Def =
        req.copy(decltpe = req.decltpe match {
          case Type.Apply(_, targs) => Type.Apply( Type.Name("FS"), targs)
          case _ => req.decltpe
        })

      val t: Trait = q" trait StackSafe { ..${requestDecls.map(withFS)} } "
      FreeAlgebra(Clait(Seq.empty[Mod], Type.Name("StackSafe"), allTParams, t.ctor, t.templ))
    }
    lazy val stackSafeT: Trait = stackSafeAlg.enrich.toTrait
    lazy val stackSafeD: Object = stackSafeAlg.mkCompanion

    val deriveDef: Defn.Def = {
      val deriveTTs = mm.paramK +: nn.paramK +: tailTParams
      val nnTTs     = nn +: tailTNames
      q"""
        implicit def derive[..$deriveTTs](
          implicit h: $name[$mm, ..$tailTNames],
          fk: _root_.cats.arrow.FunctionK[$mm, $nn]
      ): $name[$nn, ..$tailTNames] = h.mapK[$nn](fk)
      """
    }

    val functorKDef: Stat = {
      val hh = Term.fresh("hh$")
      val functorK = Term.fresh("functorKInstance$")

      val mapkdef: Defn.Def = q"""
        def mapK[${mm.paramK}, ${nn.paramK}]($hh: $name[$mm, ..$tailTNames])(
          fk: _root_.cats.arrow.FunctionK[$mm, $nn]): $name[$nn, ..$tailTNames] =
            $hh.mapK(fk)
      """

      if (tailTParams.isEmpty)
        q"""
          implicit val ${functorK.toVar}: _root_.mainecoon.FunctorK[$name] =
            new _root_.mainecoon.FunctorK[$name] {
              $mapkdef
            }"""
      else {
        val tproj: Type = q"X[({ type λ[α[_]] = $name[α, ..$tailTNames] })#λ]".targs.head
        q"""
          implicit def $functorK[..$tailTParams]: _root_.mainecoon.FunctorK[$tproj] =
            new _root_.mainecoon.FunctorK[$tproj] {
              $mapkdef
            }"""
      }
    }

    val nstats = Seq(clait.applyDef, functorKDef, deriveDef, handlerT) ++
      ( if (isStackSafe) Seq(stackSafeT, stackSafeD) else Seq() )

    val prot = q"object X {}"
    prot.copy(
      name = Term.Name(name.value),
      templ = prot.templ.copy( stats = Some(nstats) ))
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

// $COVERAGE-ON$
