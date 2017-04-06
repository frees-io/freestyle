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

import scala.reflect.internal._
import scala.reflect.macros.whitebox
import scala.reflect.runtime.universe._

object coproductcollect {

  def apply[A](a: A): Any = macro materializeImpl[A]

  def materializeImpl[A](c: whitebox.Context)(a: c.Expr[A])(
      implicit foo: c.WeakTypeTag[A]): c.Expr[Any] = {
    import c.universe._

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    // s: the companion object of the `@module`-trait
    def findAlgebras(s: ClassSymbol): List[Type] = {

      // cs: the trait annotated as `@module`
      def methodsOf(cs: ClassSymbol): List[MethodSymbol] =
        cs.info.decls.toList.collect { case met: MethodSymbol if met.isAbstract => met }

      // cs: the trait annotated as `@module`
      def isModuleFS(cs: ClassSymbol): Boolean =
        cs.companion.asModule.moduleClass.asClass.baseClasses.exists {
          case x: ClassSymbol => x.name == TypeName("FreeModuleLike")
        }

      // cs: the trait annotated as `@module`
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

    def mkCoproduct(algebras: List[Type]): List[TypeDef] =
      algebras.map(x => TermName(x.toString) ) match {
        case Nil => List( q"type Op[A] = Nothing")

        case List(alg) => List(q"type Op[A] = $alg.Op[A]")

        case alg0 :: alg1 :: algs =>
          val num = algebras.length

          def copName(pos: Int): TypeName = TypeName( if (pos + 1 != num) s"C$pos" else "Op" )

          val copDef1 = q"type ${copName(1)}[A] = cats.data.Coproduct[$alg1.Op, $alg0.Op, A]"

          def copDef(alg: Type, pos: Int ) : TypeDef =
            q"type ${copName(pos)}[A] = cats.data.Coproduct[$alg.Op, ${copName(pos-1)}, A]"

          val copDefs = algebras.zipWithIndex.drop(2).map { case (alg, pos) => copDef(alg, pos) }
          copDef1 :: copDefs
      }

    // The Main Part
    val algebras   = findAlgebras(weakTypeOf[A].typeSymbol.companion.asClass)
    val coproducts = mkCoproduct(algebras)
    //ugly hack because as String it does not typecheck early which we need for types to be in scope
    val parsed     = coproducts.map( cop => c.parse(cop.toString))

    c.Expr[Any]( q"new { ..$parsed }" )
  }

}

object moduleImpl {

  object messages {

    val invalid = "Invalid use of the `@module` annotation"

    val abstractOnly = "The `@module` macro annotation can only be applied to a trait or an abstract class."

    val noCompanion = "The trait (or abstract class) annotated with `@module` must have no companion object."
  }

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.universe.Tree = {
    import c.universe._
    import c.universe.internal.reificationSupport._

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    def filterImplicitVars( trees: Template): List[ValDef]  =
      trees.collect { case v: ValDef if v.mods.hasFlag(Flag.DEFERRED) => v }

    def mkCompanion( userTrait: ClassDef): ModuleDef = {
      val name = userTrait.name
      val implicits: List[ValDef] = filterImplicitVars(userTrait.impl)
      //  :+ q"val I: Inject[T, F]"

      val LL = freshTypeName("LL$")
      val AA = freshTypeName("AA$")
      val ev = freshTermName("ev$")
      val xx = freshTermName("xx$")

      q"""
        object ${name.toTermName} extends FreeModuleLike {
          val $xx = coproductcollect.apply(this)

          type Op[$AA] = $xx.Op[$AA]

          class To[F[_]](implicit ..$implicits) extends $name[F]

          implicit def to[F[_]](implicit ..$implicits): To[F] = new To[F]()

          def apply[$LL[_]](implicit $ev: $name[$LL]): $name[$LL] = $ev
        }
      """
    }

    // The main part
    annottees match {
      case Expr(cls: ClassDef) :: Nil =>
        if (cls.mods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) {
          val userTrait = cls.duplicate
          q"""
            $userTrait
            ${mkCompanion(userTrait)}
          """
        } else
          fail( s"${messages.invalid} in ${cls.name}. ${messages.abstractOnly}")

      case Expr(cls: ClassDef) :: Expr(_) :: _ =>
        fail( s"${messages.invalid} in ${cls.name}. ${messages.noCompanion}")

      case _ => fail( s"${messages.invalid}. ${messages.abstractOnly}")
    }
  }
}
