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

package tagless.internal

import freestyle.FreeS
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{ Class, Trait, Object }
import freestyle.internal.ScalametaUtil._

object taglessMetaImpl {
  import Util._

  def tagless(defn: Any): Stat = defn match {
    case cls: Trait =>
      freeAlg( Algebra(cls.mods, cls.name, cls.tparams, cls.ctor, cls.templ), true)
    case cls: Class if isAbstract(cls) =>
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
      Term.Block( Seq( enriched, alg.mkObject ))
    }

}

case class Algebra(
  mods: Seq[Mod],
  name: Type.Name,
  tparams: Seq[Type.Param],
  ctor: Ctor.Primary,
  templ: Template
){
  import Util._

  def toTrait: Trait = Trait(mods, name, tparams, ctor, templ)
  def toClass: Class = Class(mods, name, tparams, ctor, templ)

  val requestDecls: Seq[Decl.Def] = templ.stats.get.collect {
    case dd: Decl.Def => dd.decltpe match {
      case Type.Apply(Type.Name("FS"), args) => dd
      case _ => abort(s"$invalid in definition of method ${dd.name} in $name. $onlyReqs")
    }
  }

  def enrich: Algebra = ???

//    def mkEffectTrait(effectTrait: ClassDef): ClassDef = {
//      val requests: List[Request] = collectRequests
//      val body = requests.map(_.traitDef(FF))
//      // this is to make a TypeDef for `$FF[_]`
//      val ffTParam = TypeDef(Modifiers(Flag.PARAM), FF, List(wildcard), TypeBoundsTree(EmptyTree, EmptyTree))
//      val ClassDef(mods, name, tparams, Template(parents, self, _)) = effectTrait
//      ClassDef(mods, name, ffTParam :: tparams, Template(parents, self, body))
//    }
//


  def collectRequests: List[Request] = ???
  //templ.collect {
  //      case dd@q"$mods def $name[..$tparams](...$paramss): $tyRes" => tyRes match {
  //        case tq"FS[..$args]" => new Request(dd.asInstanceOf[DefDef])
  //        case _ => fail(s"$invalid in definition of method $name in ${effectTrait.name}. $onlyReqs")
  //      }
  //    }

  val requests: Seq[Request] = requestDecls.map( dd => new Request(dd) )

  def mkObject: Object = {
    val mm = Type.fresh("MM$")
    val nn = Type.fresh("NN$")
    val hh = Term.fresh("hh$")

    val stackSafeHandler = Term.fresh("stackSafeHandler$")
    val stackSafeFTHandler = Term.fresh("stackSafeFTHandler$")
    val functorK = Term.fresh("functorKInstance$")

    val runTParams: Seq[Type.Param] = tyParamK(mm) +: tparams
    val runTArgs: Seq[Type] = mm +: tparams.map(toType)

    val sup: Term.ApplyType = Term.ApplyType( Ctor.Ref.Name(name.value), runTArgs)

    val handlerT: Trait = q"""
      trait Handler[..$runTParams] extends $sup {
         ..${requests.map(_.handlerDef)}
      }
    """

    val stackSafeT: Trait = q"""
      @_root_.freestyle.free 
      trait StackSafe {
        ..${requests.map(_.freeDef)}
      }
    """

//    val stackSafeD: Defn.Def = q"""
//      implicit def $stackSafeHandler[$mm[_]: _root_.cats.Monad](implicit $hh: Handler[$mm]): StackSafe.Handler[$mm] =
//        new StackSafe.Handler[$mm] {
//          ..${requests.map(_.freeHandlerDef(hh, mm))}
//        }
//    """

    val deriveDef: Defn.Def = {
      val deriveTTs = tyParamK(mm) +: tyParamK(nn) +: tparams
      val nnTTs = nn +: tparams.map(toType)
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

//    val functorKDef: Defn.Val = ???
//    q"""
//      implicit val $functorK: _root_.mainecoon.FunctorK[({ type λ[α[_]] = $name[α, ..$TTs] })#λ] =
//        new _root_.mainecoon.FunctorK[({ type λ[α[_]] = $name[α, ..$TTs] })#λ] {
//          def mapK[$MM[_], $NN[_]]($hh: $name[$MM, ..$TTs])(fk: _root_.cats.arrow.FunctionK[$MM, $NN]): $name[$NN, ..$TTs] =
//            new $name[$NN, ..$TTs] {
//              ..${requests.map(_.functorKDef(hh, NN))}
//            }
//        }"""


    val prot = q"object X {}"
    prot.copy(name = Term.Name(name.value), templ = prot.templ.copy(
      stats = Some( Seq( applyDef, /*functorKDef,*/ deriveDef, stackSafeT, /* stackSafeD, */ handlerT))
    ))
  }


}

class Request(reqDef: Decl.Def) {

  import reqDef.{tparams, paramss}

  // Name of the Request ADT Class
  private[this] val reqName: String = reqDef.name.value
  private[this] val req: Type.Name = Type.Name(reqName.capitalize + "OP")

  private[this] val res: Type = reqDef.decltpe match {
    case Type.Apply(_, args) => args.last
    case _ => abort("Internal @tagless failure. Attempted to do request of non-applied type")
  }

  private[this] val reqC = Term.Name(req.value)
  private[this] val reqImpl = Term.Name(reqName)

  val params: Seq[Term.Param] = reqDef.paramss.flatten


  /*
  def freeDef: DefDef =
    if (params.isEmpty)
      q"def $reqImpl[..$tparams]: FS[$Res]"
    else
      q"def $reqImpl[..$tparams](..$params): FS[$Res]"
  */
  def freeDef: Decl.Def =  ???


 /*
  def freeHandlerDef(H: TermName, RT: TypeName): DefDef = {
    val args = params.map(_.name)
    if (params.isEmpty)
      q"def $reqImpl[..$tparams]: $RT[$Res] = $H.${reqDef.name}(..$args)"
    else
      q"def $reqImpl[..$tparams](..$params): $RT[$Res] = $H.${reqDef.name}(..$args)"
  }
  */
  def freeHandlerDef(h: Term.Name, RT: Type.Name): Defn.Def = ???

  /*
  def functorKDef(H: TermName, RT: TypeName): DefDef = {
    val args = params.map(_.name)
    if (params.isEmpty)
      q"def $reqImpl[..$tparams]: $RT[$Res] = fk($H.${reqDef.name}(..$args))"
    else
      q"def $reqImpl[..$tparams](..$params): $RT[$Res] = fk($H.${reqDef.name}(..$args))"
  }
   */
  def functorKDef(hH: Term.Name, rt: Type.Name): Defn.Def = ???

  /*
  def traitDef(FF: TypeName): DefDef =
    if (params.isEmpty)
      q"def $reqImpl[..$tparams]: $FF[$Res]"
    else
      q"def $reqImpl[..$tparams](..$params): $FF[$Res]"
   */
  def traitDef(ff: Type.Name): Decl.Def = ???

  /*
  def handlerDef: DefDef =
    if (params.isEmpty)
      q"def $reqImpl[..$tparams]: $MM[$Res]"
    else
      q"def $reqImpl[..$tparams](..$params): $MM[$Res]"
 */
  def handlerDef: Decl.Def = ???

}

object Util {
  // Messages of error
  val invalid = "Invalid use of the `@tagless` annotation"
  val abstractOnly = "The `@tagless` annotation can only be applied to a trait or to an abstract class."
  val noCompanion = "The trait (or class) annotated with `@tagless` must have no companion object."
  val onlyReqs = "In a `@tagless`-annotated trait (or class), all abstract method declarations should be of type FS[_]"
  val nonEmpty = "A `@tagless`-annotated trait or class  must have at least one abstract method of type `FS[_]`"
}