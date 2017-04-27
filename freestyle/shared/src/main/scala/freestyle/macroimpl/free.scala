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

package freestyle.macroimpl

import freestyle.FreeS
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{ Class, Trait, Object }

trait EffectLike[F[_]] {
  final type FS[A] = FreeS.Par[F, A]
  final object FS {
    final type Seq[A] = FreeS[F, A]
    final type Par[A] = FreeS.Par[F, A]
  }
}

object freeImpl {

  import errors._

  def free(defn: Any): Stat = defn match {

    case cls: Trait =>
      freeAlg( Algebra(cls.mods, cls.name, cls.tparams, cls.ctor, cls.templ), true)

    case cls: Class if ScalametaUtil.isAbstract(cls) =>
      freeAlg( Algebra(cls.mods, cls.name, cls.tparams, cls.ctor, cls.templ), false)

    case c: Class /* ! isAbstract */ => abort( s"$invalid in ${c.name}. $abstractOnly" )
    case Term.Block( Seq( _, c: Object) ) => abort( s"$invalid in ${c.name}. $noCompanion")
    case _ => abort(s"$invalid. $abstractOnly")
  }

  def freeAlg(alg: Algebra, isTrait: Boolean): Term.Block =
    if (alg.requestDecls.isEmpty)
      abort(s"$invalid in ${alg.name}. $nonEmpty")
    else {
      val enriched = if (isTrait) alg.enrich.toTrait else alg.enrich.toClass
      Term.Block( Seq( enriched, alg.mkCompanion ))
    }

}

private[macroimpl] case class Algebra(
  mods: Seq[Mod],
  name: Type.Name,
  tparams: Seq[Type.Param],
  ctor: Ctor.Primary,
  templ: Template
){
  //An Algebra has the same members as a Class or Trait in Scalameta: it abstracts on both */

  import ScalametaUtil._
  import errors._

  def toTrait: Trait = Trait(mods, name, tparams, ctor, templ)
  def toClass: Class = Class(mods, name, tparams, ctor, templ)

  /* The enrich method adds a kind-1 type parameter `$ff[_]` to the algebra type,
   * and adds `EffectLike[$ff]` to the parents */
  def enrich: Algebra = {
    val ff: Type.Name = Type.fresh("FF$")
    val pat = q"trait Foo[${tparamK(ff)}] extends _root_.freestyle.macroimpl.EffectLike[$ff]"
    Algebra(mods, name, pat.tparams, ctor, templ.copy(parents = pat.templ.parents))
  }

  val requestDecls: Seq[Decl.Def] = templ.stats.get.collect {
    case dd: Decl.Def => dd.decltpe match {
      case Type.Apply(Type.Name("FS"), args) => dd
      case _ => abort(s"$invalid in definition of method ${dd.name} in $name. $onlyReqs")
    }
  }

  val requests: Seq[Request] = requestDecls.zipWithIndex.map { case (dd, ix) => new Request(dd, ix) }

  val OP = Type.Name("Op") // Root trait of the Effect ADT
  val indexName = Term.fresh("FSAlgebraIndex")

  def handlerTrait: Trait = {
    val mm = Type.fresh("MM$")

    val applyDef: Defn.Def = {
      val aa = Type.fresh("AA$")
      val errorCase: Case =
        q"""
          x match { case i => throw new _root_.java.lang.Exception(
            s"freestyle internal error: index " + i + " out of bounds for " + this)
          }
        """.cases.head

      val fa = Term.fresh("fa$")
      val matchE: Term.Match = Term.Match(
        q"$fa.$indexName : @_root_.scala.annotation.switch",
        requests.map(_.handlerCase(fa)) :+ errorCase
      )

      q"override def apply[${tparam(aa)}]($fa: $OP[$aa]): $mm[$aa] = ($matchE).asInstanceOf[$mm[$aa]]"
    }

    q"""
      trait Handler[${tparamK(mm)}, ..$tparams] extends _root_.freestyle.FSHandler[$OP, $mm] {
        ..${requests.map(_.handlerDef(mm))}
        $applyDef
      }
    """
  }

  def mkCompanion: Object = {

    val (toClass, toDef, applyDef): (Class, Defn.Def, Defn.Def) = {
      val gg: Type.Name = Type.fresh("LL$") // LL is the target of the Lifter's Injection
      val injTParams: Seq[Type.Param] = tparamK(gg) +: tparams
      val injTArgs: Seq[Type] = gg +: tparams.map(toType)
      val ii = Term.fresh("ii$")

      val toClass: Class = {
        val inj = Term.fresh("toInj")
        def injPat = Pat.Var.Term(inj)

        val sup: Term.ApplyType = Term.ApplyType( Ctor.Ref.Name(name.value), injTArgs)
        q"""
          class To[..$injTParams](implicit $ii: _root_.freestyle.InjK[$OP, $gg]) extends $sup {
            private[this] val $injPat = _root_.freestyle.FreeS.inject[$OP, $gg]($ii)
            ..${requests.map(_.toDef(inj) )}
          }
        """
      }

      val toDef: Defn.Def =
        q"implicit def to[..$injTParams](implicit $ii: _root_.freestyle.InjK[$OP, $gg]): To[..$injTArgs] = new To[..$injTArgs]"

      val ev = Term.fresh("ev$")

      val applyDef: Defn.Def =
        q"def apply[..$injTParams](implicit $ev: $name[..$injTArgs]): $name[..$injTArgs] = $ev"

      (toClass, toDef, applyDef)
    }

    val opTrait =
      q"""
        sealed trait $OP[_] extends scala.Product with java.io.Serializable {
          val ${toVar(indexName)} : _root_.scala.Int
        }
      """

    val opTypes = q"type OpTypes = _root_.iota.KCons[$OP, _root_.iota.KNil]"

    val adt: Seq[Stat] = opTrait +: requests.map(_.reqClass(OP, tparams, indexName))

    val prot = q"object X {}"
    prot.copy(name = Term.Name(name.value), templ = prot.templ.copy(
      stats = Some(adt ++ Seq( opTypes, handlerTrait, toClass, toDef, applyDef) )
    ))
  }

}

private[macroimpl] class Request(reqDef: Decl.Def, indexValue: Int) {

  import reqDef.{tparams, paramss}
  import ScalametaUtil._

  // Name of the Request ADT Class
  private[this] val reqName: String = reqDef.name.value
  private[this] val req: Type.Name = Type.Name(reqName.capitalize + "OP")

  private[this] val res: Type = reqDef.decltpe match {
    case Type.Apply(_, args) => args.last
    case _ => abort("Internal @free failure. Attempted to do request of non-applied type")
  }

  private[this] val reqC = Term.Name(req.value)
  private[this] val reqImpl = Term.Name(reqName)

  val params: Seq[Term.Param] = reqDef.paramss.flatten

  def reqClass(OP: Type.Name, effTTs: Seq[Type.Param], indexName: Term.Name): Class = {
    val tts = effTTs ++ tparams
    val sup: Term.ApplyType = Term.ApplyType( Ctor.Ref.Name(OP.value), Seq(res)) // this means $OP[$res]
    val ix = Pat.Var.Term(indexName)
    q"""
      case class $req[..$tts](..$params) extends AnyRef with $sup {
        override val $ix: _root_.scala.Int = $indexValue
      }
    """
  }

  def toDef(inj: Term.Name): Defn.Def = {
    val body: Term =
      if (tparams.isEmpty)
        q"$reqC(..${params.map(toName)})"
      else
        q"$reqC[..${tparams.map(toType)} ](..${params.map(toName)})"

    addBody(reqDef, q"$inj($body)").copy( mods = Seq(Mod.Override() ) )
  }

  def handlerCase(fa: Term.Name): Case = {

    val mexp: Term =
      if (params.isEmpty)
        q"$reqImpl"
      else {
        val tt: Type =
          if (tparams.isEmpty) req else {
            val us: Type = Type.Placeholder(Type.Bounds(None, None) )
            Type.Apply(req, tparams.map(t => us))
          }

        val alias = Term.fresh()
        val ffs = params.map( v => q"$alias.${toName(v)}")
        q"""
          val ${toVar(alias)}: $tt = $fa.asInstanceOf[$tt]
          $reqImpl(..$ffs)
        """
      }

    Case( Lit.Int(indexValue), None, mexp) // case ix => mexp
  }

  def handlerDef(mm: Type.Name): Decl.Def =
    if (params.isEmpty)
      q"protected[this] def $reqImpl[..$tparams]: $mm[$res]"
    else
      q"protected[this] def $reqImpl[..$tparams](..$params): $mm[$res]"

}

private[macroimpl] object errors {
  // Messages of error
  val invalid = "Invalid use of `@free`"
  val abstractOnly = "`@free` can only annotate a trait or abstract class"
  val noCompanion = "`@free` can only annotate a trait (or class) without companion"
  val onlyReqs = "In a `@free`-trait (or class), all abstract methods declarations should be of type FS[_]"
  val nonEmpty = "A `@free` trait or class  must have at least one abstract method of type `FS[_]`"
}

