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

package freestyle

import scala.reflect.macros.whitebox.Context

import cats.data._
import cats.syntax.validated._
import cats.syntax.traverse._
import cats.instances.list._

trait FreeModuleLike

final class moduleImpl(val c: Context) {
  import c.universe._
  import c.universe.internal.reificationSupport._

  // Error Messages
  val invalid = "Invalid use of the `@module` annotation"
  val abstractOnly = "The `@module` annotation can only be applied to a trait or an abstract class."
  val noCompanion = "The trait or class annotated with `@module` must have no companion object."

  val LL = freshTypeName("LL$")
  val AA = freshTypeName("AA$")

  def fail(msg: String) = c.abort(c.enclosingPosition, msg)

  def impl(annottees: c.Expr[Any]*): c.universe.Tree =
    annottees match {
      case Expr(cls: ClassDef) :: Nil =>
        if (cls.mods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) {
          q"""
            ${mkModuleTrait(cls.duplicate)}
            ${mkModuleObject(cls.duplicate)}"""
        } else fail( s"$invalid in ${cls.name}. $abstractOnly")

      case Expr(cls: ClassDef) :: Expr(_) :: _ => fail( s"$invalid in ${cls.name}. $noCompanion")

      case _ => fail( s"$invalid. $abstractOnly")
    }

  def filterEffectVals(trees: Template): List[ValDef]  =
    trees.collect { case v: ValDef if v.mods.hasFlag(Flag.DEFERRED) => v }

  def toImplArg(effVal: ValDef): ValDef = effVal match {
    case q"$mods val $name: $eff[..$args]" =>
      q"$mods val $name: $eff[$LL, ..$args]"
  }

  def mkModuleTrait(cls: ClassDef): ClassDef = {
    val FF = freshTypeName("FF$")
    // this is to make a TypeDef for `$FF[_]`
    val wildcard = TypeDef(Modifiers(Flag.PARAM), typeNames.WILDCARD, List(), TypeBoundsTree(EmptyTree, EmptyTree))
    val ffTParam = TypeDef(Modifiers(Flag.PARAM), FF, List(wildcard), TypeBoundsTree(EmptyTree, EmptyTree))

    val ClassDef(mods, name, tparams, Template(parents, self, body)) = cls

    val nbody = body.map {
      case q"$mods val $name: $eff[..$args]" => q"$mods val $name: $eff[$FF, ..$args]"
      case x => x
    }
    val nimpl = Template(parents :+ tq"freestyle.FreeModuleLike", self, nbody)
    ClassDef(mods, name, ffTParam :: tparams, nimpl)
  }

  def moduleAlgebras(userTrait: ClassDef): ValidatedNel[String, List[Type]] = {

    def isModuleFS(cs: ClassSymbol): Boolean =
      cs.baseClasses.exists( _.name == TypeName("FreeModuleLike") )

    def expandClass(cs: ClassSymbol): List[Type] =
      cs.info.decls.toList
        .collect { case met: MethodSymbol if met.isAbstract => met }
        .flatMap(x => expandMethod(x.returnType))

    def expandMethod(meth: Type): List[Type] =
      meth.typeSymbol match {
        case cs: ClassSymbol =>
          if (isModuleFS(cs)) expandClass(cs) else List(meth.typeConstructor)
        case _ => Nil
      }

    userTrait.impl.body
      .collect { case q"val $name: $tpt" => tpt }
      .traverse { tpt =>
        val tpe = c.typecheck(tpt, mode = c.TYPEmode, silent = true).tpe
        if (tpe != null) tpe.validNel
        else
          s"unable to typecheck tree $tpt in user trait $userTrait".invalidNel
      }
      .map(_.flatMap(expandMethod))
  }

  def mkCoproduct(algebras: List[Type]): TypeDef =
    algebras.map(_.typeSymbol) match {
      case Nil => q"type Op[$AA] = Nothing"

      case alg :: Nil => q"type Op[$AA] = $alg.Op[$AA]"

      case alg0 :: alg1 :: algs =>
        /* We create several type aliases, where the type names are generated fresh names,
         * C0, C1, ..., C{n-1}, where n is the length of algebras. We then generate aliases:
         *
         * type C1 = A1 |+| A0
         * type C2 = A2 |+| C1
         * type Ci = Ai |+| C{i-1}
         * type C{n-1} = A{n-1} |+| C{n-2
         * type Op{n-1}= C{n-1}
         */
        val ccs: List[TypeName] = algebras.map( _ => freshTypeName("CC$") )
        val tyDef1 = q"type ${ccs(1)}[$AA] = _root_.cats.data.Coproduct[$alg1.Op, $alg0.Op, $AA]"
        val tyDefs = algebras.zipWithIndex.drop(2).map { case (alg, pos) =>
          q"type ${ccs(pos)}[$AA] = _root_.cats.data.Coproduct[$alg.Op, ${ccs(pos-1)}, $AA]"
        }
        //val opDef = q"type Op[$AA] = ${ccs.last}[$AA]"
        //tyDef1 :: (tyDefs :+ opDef)

        q"""type Op[$AA] = ({
             $tyDef1
             ..$tyDefs
            })#${ccs.last}[$AA]"""
    }


  /* This method takes as input the `ClassSymbol` of the `@module`-annotated `trait` and computes the list
   *  of _algebras_ (`@free`-annotated `trait`s) used, directly or transitively, by the `@module`.
   *
   * To recognise if a `val` type is itself  a `@module`, the `@module` macro adds `FreeModuleLike`
   * trait as a super-class of the `@module`-annotated trait.
   */
  def findAlgebras(s: ClassSymbol): List[Type] = {

    def methodsOf(cs: ClassSymbol): List[MethodSymbol] =
      cs.info.decls.toList.collect { case met: MethodSymbol if met.isAbstract => met }

    def isModuleFS(cs: ClassSymbol): Boolean =
      cs.baseClasses.exists( _.name == TypeName("FreeModuleLike") )

    def fromClass(cs: ClassSymbol): List[Type] =
      methodsOf(cs).flatMap(x => fromMethod(x.returnType))

    def fromMethod(meth: Type): List[Type] =
      meth.typeSymbol match {
        case cs: ClassSymbol =>
          if (isModuleFS(cs)) fromClass(cs) else List(meth.typeConstructor)
        case _ => Nil
      }

    fromClass(s)
  }

  def mkModuleObject(userTrait: ClassDef): ModuleDef = {
    val mod = userTrait.name
    val tts = userTrait.tparams
    val tns = tts.map(_.name)
    val AA = freshTypeName("AA$")
    val ev = freshTermName("ev$")
    val xx = freshTermName("xx$")

    val effArgs: List[ValDef] = filterEffectVals(userTrait.impl).map( v => toImplArg(v) )

    val coproduct = foldAbort(for {
      algebras <- moduleAlgebras(userTrait)
      _coproduct = mkCoproduct(algebras)
    } yield c.parse(showCode(_coproduct)))

    q"""
     object ${mod.toTermName} {

       $coproduct

       class To[$LL[_], ..$tts](implicit ..$effArgs) extends $mod[$LL, ..$tns]

       implicit def to[$LL[_], ..$tts](implicit ..$effArgs): To[$LL, ..$tns] = new To[$LL, ..$tns]()

       def apply[$LL[_], ..$tts](implicit $ev: $mod[$LL, ..$tns]): $mod[$LL, ..$tns] = $ev
     }"""

  }

  private[this] def foldAbort[A](v: ValidatedNel[String, A]): A =
    v fold (
      errors => c.abort(c.enclosingPosition, errors.toList.mkString(", and\n")),
      a      => a)
}
