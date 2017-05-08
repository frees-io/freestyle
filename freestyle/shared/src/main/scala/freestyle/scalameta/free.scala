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

package freestyle.scalameta

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{ Class, Trait, Object }

object errors {
  // Messages of error
  val invalid = "Invalid use of `@free`"
  val abstractOnly = "`@free` can only annotate a trait or abstract class"
  val noCompanion = "`@free` can only annotate a trait (or class) without companion"
  val onlyReqs = "In a `@free`-trait (or class), all abstract methods declarations should be of type FS[_]"
}

object freeImplMeta {

  import conversions._
  import errors._

  def free(defn: Any): Stat = defn match {
    case cls: Trait =>
      val alg = Algebra(cls)
      Term.Block( Seq( enrichAlgebra(alg).toTrait, mkCompanion(alg) ) )
    case cls: Class if isAbstract(cls) =>
      val alg = Algebra(cls)
      Term.Block( Seq( enrichAlgebra(alg).toClass, mkCompanion(alg) ))

    case c: Class /* ! isAbstract */ => abort( s"$invalid in ${c.name}. $abstractOnly" )
    case Term.Block( Seq( _, c: Object) ) => abort( s"$invalid in ${c.name}. $noCompanion")
    case _ => abort(s"$invalid. $abstractOnly")
  }

  def isAbstract(cls: Class): Boolean = cls.mods.exists {
    case Mod.Abstract() => true
    case _ => false
  }

  def enrichAlgebra(alg: Algebra): Algebra = {
    // Changes to do: add extra higher-kinded parameter, add 
    val ff: Type.Name = Type.fresh("FF$")
    val ffTParam = toParamHK(ff)
    val sup: Term.ApplyType = Term.ApplyType(
      Ctor.Ref.Select( Term.Name("freestyle"), Ctor.Ref.Name("EffectLike") ), Seq(ff)
    )
    import alg.templ
    alg.copy( 
      tparams = ffTParam +: alg.tparams, 
      templ = templ.copy( parents = templ.parents :+ sup)
    )
  }

  def mkCompanion(alg: Algebra): Object = {
    import alg.tparams

    val targs = tparams.map(toType)
    val Eff = Term.Name(alg.name.value)
    val OP = Type.Name("Op") // Root trait of the Effect ADT
    val ii = Term.fresh("ii$")
    val requests: Seq[Request] = alg.collectRequests

    val handler: Trait = {
      val AA = Type.fresh("AA$") // AA is the parameter inside type applications
      val MM = Type.fresh("MM$")
      val fa = Term.fresh("fa$")
      q"""
        trait Handler[${toParamHK(MM)}, ..$tparams] extends FunctionK[$OP, $MM] {
          { ..${requests.map(_.handlerDef(MM))} }

          override def apply[${toParam(AA)}]($fa: $OP[$AA]): $MM[$AA] = $fa match {
            ..case ${requests.map(_.handlerCase)}
          }
        }
        """
    }

    val LL = Type.fresh("LL$") // LL is the target of the Lifter's Injection
    val lifter: Class = {
      val inj = Term.fresh("toInj")
      def injPat = Pat.Var.Term(inj)

      // $Eff[$LL, ..$tparams]
      val sup: Term.ApplyType = Term.ApplyType( Ctor.Ref.Name(Eff.value) , LL +: targs )
      q"""
        class To[${toParamHK(LL)}, ..$tparams](implicit $ii: Inject[$OP, $LL]) extends AnyRef with $sup {
          private[this] val $injPat = FreeS.inject[$OP, $LL]($ii)
          ..${requests.map(_.lifter(inj) )}
        }
      """
    }

    val ev = Term.fresh("ev$")
    q"""
      object $Eff {
        import _root_.cats.arrow.FunctionK
        import _root_.cats.free.Inject
        import _root_.freestyle.FreeS

        sealed trait $OP[_] extends scala.Product with java.io.Serializable
        { ..${requests.map(_.reqClass(OP, tparams))} }
        { $handler }
        { $lifter  }

        implicit def to[${toParamHK(LL)}, ..$tparams](implicit $ii: Inject[$OP, $LL]):
          To[$LL, ..$targs] = new To[$LL, ..$targs]

        def apply[${toParamHK(LL)}, ..$tparams]( implicit $ev: ${alg.name}[$LL, ..$targs]): 
          ${alg.name}[$LL, ..$targs] = $ev 
      }
    """

  }

}


case class Algebra( mods: Seq[Mod], name: Type.Name, tparams: Seq[Type.Param], ctor: Ctor.Primary, templ: Template ){
  def toTrait: Trait = Trait(mods, name, tparams, ctor, templ)
  def toClass: Class = Class(mods, name, tparams, ctor, templ)

  import errors._

  def collectRequests: Seq[Request] = templ.stats.get.collect {
    case dd: Decl.Def => dd.decltpe match {
      case Type.Apply(Type.Name("FS"), args) => new Request(dd)
      case _ => abort(s"$invalid in definition of method ${dd.name} in $name. $onlyReqs")
    }
  }

}

object Algebra {
  def apply(cls: Class): Algebra = apply(cls.mods, cls.name, cls.tparams, cls.ctor, cls.templ)
  def apply(cls: Trait): Algebra = apply(cls.mods, cls.name, cls.tparams, cls.ctor, cls.templ)
}

class Request(reqDef: Decl.Def) {

  import reqDef.{tparams, paramss}
  import conversions._

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

  def handlerCase: Case = {
    val extract: Pat = Pat.Extract(reqC, Nil, params.map( _ => Pat.Wildcard()) )
    if (params.isEmpty)
      Case( extract , None, q"$reqImpl()")
    else {
      val alias = Term.fresh()
      val ffs = params.map( v => q"$alias.${toName(v)}")
      Case( Pat.Bind( Pat.Var.Term(alias), extract), None, q"$reqImpl(..$ffs)" )
    }
  }

  def handlerDef(MM: Type.Name): Decl.Def =
    if (tparams.isEmpty)
      q"protected[this] def $reqImpl[..$tparams](..$params): $MM[$res]"
    else
      q"protected[this] def $reqImpl[..$tparams](..$params): $MM[$res]"

  def lifter(inj: Term.Name): Defn.Def =
    if (tparams.isEmpty)
      q"""
        override def ${reqDef.name}(...${reqDef.paramss}): ${reqDef.decltpe} =
          $inj( $reqC(..${params.map(toName)}))
      """
    else
      q"""
        override def ${reqDef.name}[..$tparams](...${reqDef.paramss}): ${reqDef.decltpe} =
          $inj( $reqC[..${tparams.map(toType)} ](..${params.map(toName)}))
      """

  def reqClass(OP: Type.Name, effTTs: Seq[Type.Param]): Class = {
    val tts = effTTs ++ tparams
    val sup: Term.ApplyType = Term.ApplyType( Ctor.Ref.Name(OP.value), Seq(res)) // this means $OP[$res] //
    q"case class $req[..$tts](..$params) extends AnyRef with $sup {}"
  }

}

object conversions {

  private[this] val unbound = Type.Bounds( None, None) 

  def toName(par: Term.Param): Term.Name = Term.Name(par.name.value)

  def toType(par: Type.Param): Type = Type.Name(par.name.value)

  def toParam(name: Type.Name): Type.Param = Type.Param(Nil, name, Nil, unbound, Nil, Nil)

  def toParamHK(tn: Type.Name): Type.Param = {
    val wildcard = Type.Param(Nil, Name.Anonymous(), Nil, unbound, Nil, Nil)
    Type.Param(Nil, tn, Seq(wildcard), unbound, Nil, Nil)
  }

}