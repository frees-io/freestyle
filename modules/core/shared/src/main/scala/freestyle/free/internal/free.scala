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
import scala.meta.Type.Param

trait EffectLike[F[_]] {
  final type FS[A] = FreeS.Par[F, A]
  final object FS {
    final type Seq[A] = FreeS[F, A]
    final type Par[A] = FreeS.Par[F, A]
  }
}

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object freeImpl {

  import errors._
  import syntax._

  def free(defn: Any): Stat = defn match {

    case cls: Trait =>
      freeAlg( Algebra(Clait(cls)), isTrait = true).`debug?`(cls.mods)
    case cls: Class if ScalametaUtil.isAbstract(cls) =>
      freeAlg( Algebra(Clait(cls)), isTrait = false).`debug?`(cls.mods)

    case c: Class /* ! isAbstract */   => abort(s"$invalid in ${c.name}. $abstractOnly")
    case Term.Block(Seq(_, c: Object)) => abort(s"$invalid in ${c.name}. $noCompanion")
    case _                             => abort(s"$invalid. $abstractOnly")
  }

  def freeAlg(alg: Algebra, isTrait: Boolean): Term.Block =
    if (alg.requestDecls.isEmpty)
      abort(s"$invalid in ${alg.clait.name}. $nonEmpty")
    else {
      val enriched = if (isTrait) alg.enrich.toTrait else alg.enrich.toClass
      Term.Block(Seq(enriched, alg.mkCompanion))
    }

}

private[freestyle] case class Algebra( clait: Clait ) {
  //An Algebra has the same members as a Class or Trait in Scalameta: it abstracts on both */

  import ScalametaUtil._
  import errors._
  import clait._

  /* The enrich method adds a kind-1 type parameter `$ff[_]` to the algebra type,
   * and adds `EffectLike[$ff]` to the parents */
  def enrich: Clait = {
    val ff = headTParam
    val pat = q"trait Foo[$ff] extends _root_.freestyle.free.internal.EffectLike[${ff.toName}]"
    Clait(mods, name, pat.tparams, ctor, templ.copy(parents = pat.templ.parents))
  }

  val requestDecls: Seq[Decl.Def] = templ.stats.get.collect {
    case dd: Decl.Def =>
      dd.decltpe match {
        case Type.Apply(Type.Name("FS"), args) => dd
        case _                                 => abort(s"$invalid in definition of method ${dd.name} in $name. $onlyReqs")
      }
  }

  val requests: Seq[Request] = requestDecls.zipWithIndex.map {
    case (dd, ix) => new Request(dd, ix)
  }

  val OP: Type.Name        = Type.Name("Op") // Root trait of the Effect ADT
  val indexName: Term.Name = Term.fresh("FSAlgebraIndex")

  def handlerTrait: Trait = {

    val mm = Type.fresh("MM$")

    val applyDef: Defn.Def = {
      val aa = Type.fresh("AA$")
      val errorCase: Case =
        q"""
          x match { case i => throw new _root_.java.lang.Exception(
            "freestyle internal error: index " + i.toString() + " out of bounds for " + this.toString())
          }
        """.cases.head

      val fa = Term.fresh("fa$")
      val matchE: Term.Match = Term.Match(
        q"$fa.$indexName : @_root_.scala.annotation.switch",
        requests.map(_.handlerCase(fa)) :+ errorCase
      )

      q"override def apply[${aa.param}]($fa: $OP[$aa]): $mm[$aa] = ($matchE).asInstanceOf[$mm[$aa]]"
    }

    q"""
      trait Handler[${mm.paramK}, ..$tailTParams] extends _root_.freestyle.free.FSHandler[$OP, $mm] {
        ..${requests.map(_.handlerDef(mm))}
        ..${requests flatMap (_.tparamsAsTypeDecls)}
        $applyDef
      }
    """
  }

  def lifterStats: (Class, Defn.Def) = {
    val gg: Type.Name               = Type.fresh("LL$") // LL is the target of the Lifter's Injection
    val ii                          = Term.fresh("ii$")

    val toClass: Class = {
      val inj    = Term.fresh("toInj")
      def injPat = Pat.Var.Term(inj)

      val sup: Term.ApplyType = Term.ApplyType(name.ctor, gg +: tailTNames)
      q"""
        class To[${gg.paramK}, ..$tailTParams](implicit $ii: _root_.freestyle.free.InjK[$OP, $gg]) extends $sup {
          private[this] val $injPat = _root_.freestyle.free.FreeS.inject[$OP, $gg]($ii)
          ..${requests.map(_.toDef(inj))}
        }
      """
    }

    val toDef: Defn.Def =
      q"""
        implicit def to[${gg.paramK}, ..$tailTParams](
          implicit $ii: _root_.freestyle.free.InjK[$OP, $gg]
        ): To[$gg, ..$tailTNames] = new To[$gg, ..$tailTNames]
      """

    (toClass, toDef)
  }
  val applyDefConcreteOp: Defn.Def =
    q"def instance(implicit ev: $name[$OP]): $name[$OP] = ev"


  def mkCompanion: Object = {
    val opTrait = {
      val index: Decl.Val = q"val ${toVar(indexName)} : _root_.scala.Int"
      q"sealed trait $OP[_] extends _root_.scala.Product with _root_.java.io.Serializable { $index }"
    }
    val opTypes                      = q"type OpTypes = _root_.iota.TConsK[$OP, _root_.iota.TNilK]"
    val adt: Seq[Stat]               = opTrait +: requests.map(_.reqClass(OP, tailTParams, indexName))
    val (toClass, toDef)             = lifterStats
    val prot                         = q"""@_root_.java.lang.SuppressWarnings(_root_.scala.Array(
                                           "org.wartremover.warts.Any",
                                           "org.wartremover.warts.AsInstanceOf",
                                           "org.wartremover.warts.Throw"
                                         ))
                                         object X {}"""

    prot.copy(
      name = Term.Name(name.value),
      templ = prot.templ.copy(
        stats = Some(adt ++ Seq(opTypes, handlerTrait, toClass, toDef, clait.applyDef, applyDefConcreteOp))
      ))
  }

}

private[internal] class Request(reqDef: Decl.Def, indexValue: Int) {

  import ScalametaUtil._
  import reqDef.name

  // Name of the Request ADT Class
  private[this] val req: Type.Name  = Type.Name(name.value.capitalize + "Op")
  private[this] val cboundPrefix    = "__$cbound"

  private[this] val res: Type = reqDef.decltpe match {
    case Type.Apply(_, args) => args.last
    case _                   => abort("Internal @free failure. Attempted to do request of non-applied type")
  }

  val tparamsAsTypeDecls: Seq[Decl.Type] = {
    def toDeclType(tp: Type.Param): Decl.Type =
      Decl.Type(tp.mods, Type.fresh("PP$"), tp.tparams.map(_.unboundC), tp.tbounds)

    reqDef.tparams map toDeclType
  }

  private[this] val reqC = Term.Name(req.value)

  val cbtparams: Seq[Term.Param] = {
    def cboundParam(ty: Type): Term.Param =
      Term.Param( Nil, Term.fresh(cboundPrefix), Some(ty), None)
    reqDef.tparams.flatMap( _.classBoundsToParamTypes).map(cboundParam)
  }

  val extendedParamss: Seq[Seq[Term.Param]] = reqDef.paramss.addImplicits(cbtparams)
  val classParamss: Seq[Seq[Term.Param]] = reqDef.paramss.addImplicits(cbtparams).addEmptyExplicit

  def reqClass(OP: Type.Name, effTTs: Seq[Type.Param], indexName: Term.Name): Class = {
    val tts = effTTs ++ unboundTparams
    val sup = Term.ApplyType(OP.ctor, Seq(res)) // this means $OP[$res]
    val ix = Pat.Var.Term(indexName)
    q"""
      final case class $req[..$tts](...${classParamss.map(_.toVals)}) extends _root_.scala.AnyRef with $sup {
        override val $ix: _root_.scala.Int = $indexValue
      }
    """
  }

  def reqClassApply: Term = {
    def nameOrImplicitly(param: Term.Param): Term.Name = param match {
      case Term.Param(_, paramname, Some(atpeopt), _) if paramname.value.startsWith(cboundPrefix) =>
        val targ"${tpe: Type}" = atpeopt
        Term.Name(s"_root_.scala.Predef.implicitly[$tpe]")
      case p => p.toName
    }
    val ccs = classParamss.map(_.map(nameOrImplicitly))
    if (reqDef.tparams.isEmpty)
      q"$reqC(...$ccs)"
    else
      q"$reqC[..${reqDef.tparams.map(_.toName)}](...$ccs)"
  }

  def toDef(inj: Term.Name): Defn.Def =
    reqDef.addBody( q"$inj($reqClassApply)").addMod(Mod.Override())

  def handlerCase(fa: Term.Name): Case = {

    val mexp: Term =
      if (extendedParamss.isEmpty)
        name
      else {
        val tt: Type =
          if (reqDef.tparams.isEmpty) req
          else
            // Wildcard types are not working for function params like this f: (B, A) => B
            // val us: Type = Type.Placeholder(Type.Bounds(None, None) )
            Type.Apply(req, tparamsAsTypeDecls.map(_.name))

        val alias = Term.fresh()
        val ffs   = extendedParamss.map(_.map(v => q"$alias.${v.toName}"))
        q"""
          val ${toVar(alias)}: $tt = $fa.asInstanceOf[$tt]
          $name(...$ffs)
        """
      }

    Case(Lit.Int(indexValue), None, mexp) // case ix => mexp
  }

  val unboundTparams: Seq[Param] = reqDef.tparams.map( _.unboundC)

  def handlerDef(mm: Type.Name): Decl.Def =
    if (extendedParamss.isEmpty)
      q"protected[this] def $name[..$unboundTparams]: $mm[$res]"
    else
      q"protected[this] def $name[..$unboundTparams](...$extendedParamss): $mm[$res]"

}

private[internal] object errors {
  // Messages of error
  val invalid      = "Invalid use of `@free`"
  val abstractOnly = "`@free` can only annotate a trait or abstract class"
  val noCompanion  = "`@free` can only annotate a trait (or class) without companion"
  val onlyReqs =
    "In a `@free`-trait (or class), all abstract methods declarations should be of type FS[_]"
  val nonEmpty = "A `@free` trait or class  must have at least one abstract method of type `FS[_]`"
}
// $COVERAGE-ON$
