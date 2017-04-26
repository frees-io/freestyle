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

trait FreeModuleLike

object openUnion {

  def apply[A](a: A): Any = macro materializeImpl[A]

  def materializeImpl[A](c: Context)(a: c.Expr[A])( implicit foo: c.WeakTypeTag[A]): c.Expr[Any] = {

    import c.universe._
    import c.universe.internal.reificationSupport._

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

    val AA = freshTypeName("AA$")

    def mkCoproduct(algebras: List[Type]): List[TypeDef] =
      algebras.map(x => TermName(x.toString) ) match {
        case Nil => q"type Op[$AA] = Nothing" :: Nil

        case alg :: Nil => q"type Op[$AA] = $alg.Op[$AA]" :: Nil

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
          val tyDef1 = q"type ${ccs(1)}[$AA] = Coproduct[$alg1.Op, $alg0.Op, $AA]"
          val tyDefs = algebras.zipWithIndex.drop(2).map { case (alg, pos) =>
            q"type ${ccs(pos)}[$AA] = Coproduct[$alg.Op, ${ccs(pos-1)}, $AA]"
          }
          val opDef = q"type Op[$AA] = ${ccs.last}[$AA]"
          tyDef1 :: (tyDefs :+ opDef)
      }

    // findAlgebras starts from the ClassSymbol of the `@moudle-annotated trait
    val algebras   = findAlgebras(weakTypeOf[A].typeSymbol.companion.asClass)
    val coproducts = mkCoproduct(algebras)
    //ugly hack because, as String,  it does not typecheck, early which we need for types to be in scope
    val parsed     = coproducts.map( cop => c.parse(cop.toString))

    val expr = if (algebras.length >= 2)
      q"""new {
        import _root_.cats.data.Coproduct
        ..$parsed
      }"""
    else
      q"""new {
        ..$parsed
      }"""
    c.Expr[Any](expr)
  }

}

object moduleImpl {

  def impl(c: Context)(annottees: c.Expr[Any]*): c.universe.Tree = {
    import c.universe._
    import c.universe.internal.reificationSupport._

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    def filterEffectVals(trees: Template): List[ValDef]  =
      trees.collect { case v: ValDef if v.mods.hasFlag(Flag.DEFERRED) => v }

    val LL = freshTypeName("LL$")

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

    def mkModuleObject(userTrait: ClassDef): ModuleDef = {
      val mod = userTrait.name
      val tts = userTrait.tparams
      val tns = tts.map(_.name)
      val AA = freshTypeName("AA$")
      val ev = freshTermName("ev$")
      val xx = freshTermName("xx$")

      val effArgs: List[ValDef] = filterEffectVals(userTrait.impl).map( v => toImplArg(v) )

      q"""
        object ${mod.toTermName} {

          val $xx = openUnion.apply(this)

          type Op[$AA] = $xx.Op[$AA]

          class To[$LL[_], ..$tts](implicit ..$effArgs) extends $mod[$LL, ..$tns]

          implicit def to[$LL[_], ..$tts](implicit ..$effArgs): To[$LL, ..$tns] = new To[$LL, ..$tns]()

          def apply[$LL[_], ..$tts](implicit $ev: $mod[$LL, ..$tns]): $mod[$LL, ..$tns] = $ev
        }
      """
    }

    // Error Messages
    val invalid = "Invalid use of the `@module` annotation"
    val abstractOnly = "The `@module` annotation can only be applied to a trait or an abstract class."
    val noCompanion = "The trait or class annotated with `@module` must have no companion object."

    // The main part
    annottees match {
      case Expr(cls: ClassDef) :: Nil =>
        if (cls.mods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) {
          q"""
            ${mkModuleTrait(cls.duplicate)}
            ${mkModuleObject(cls.duplicate)}
          """
        } else fail( s"$invalid in ${cls.name}. $abstractOnly")

      case Expr(cls: ClassDef) :: Expr(_) :: _ => fail( s"$invalid in ${cls.name}. $noCompanion")

      case _ => fail( s"$invalid. $abstractOnly")
    }
  }
}
