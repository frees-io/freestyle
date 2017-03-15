package freestyle

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros
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

@compileTimeOnly("enable macro paradise to expand @module macro annotations")
class module extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro module.impl
}

object module {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.universe.Tree = {
    import c.universe._
    import scala.reflect.internal._
    import internal.reificationSupport._

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    def filterImplicitVars( trees: Template): List[ValDef]  =
      trees.collect { case v: ValDef if v.mods.hasFlag(Flag.DEFERRED) => v }

    def mkCompanion( name: TypeName, implicits: List[ValDef] ): ModuleDef = {
      q"""
        object ${name.toTermName} extends FreeModuleLike {
          val X = coproductcollect.apply(this)
          type Op[A] = X.Op[A]

          class To[F[_]](implicit ..$implicits) extends $name[F]

          implicit def to[F[_]](implicit ..$implicits): To[F] = new To[F]()

          def apply[F[_]](implicit ev: $name[F]): $name[F] = ev
        }
      """
    }

    // The main part
    annottees match {
      case List(Expr(cls: ClassDef)) =>
        if (cls.mods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) {
          val userTrait @ ClassDef(_, name, _, clsTemplate) = cls.duplicate
          val implicits: List[ValDef] = filterImplicitVars(clsTemplate)
          //  :+ q"val I: Inject[T, F]"
          q"""
            $userTrait
            ${mkCompanion(name, implicits)}
          """
        } else
          fail(s"@free requires trait or abstract class")
      case _ =>
        fail(
          s"Invalid @module usage, only traits and abstract classes without companions are supported")
    }
  }
}
