package freestyle

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.reflect.runtime.universe._

trait Modular

object materialize {

  def apply[A](a: A): Any = macro materializeImpl[A]

  def materializeImpl[A](c: whitebox.Context)(a: c.Expr[A])(
      implicit foo: c.WeakTypeTag[A]): c.Expr[Any] = {
    import c.universe._

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    object log {
      def err(msg: String): Unit                = c.error(c.enclosingPosition, msg)
      def warn(msg: String): Unit               = c.warning(c.enclosingPosition, msg)
      def info(msg: String): Unit               = c.info(c.enclosingPosition, msg, force = true)
      def rawInfo(name: String, obj: Any): Unit = info(name + " = " + showRaw(obj))
    }

    def findAlgebras(s: ClassSymbol): List[Type] = {

      def isFreeStyleModule(cs: ClassSymbol): Boolean =
        cs.baseClasses.exists {
          case x: ClassSymbol => x.name == TypeName("Modular")
        }

      def fromClass(c: ClassSymbol) : List[Type] =
        c.companion.info.decls.toList.flatMap {
          case x: MethodSymbol => fromMethod(x.asMethod)
          case _ => Nil
        }

      def fromMethod(meth: MethodSymbol): List[Type] = {
        val companionClass    = meth.returnType.companion.typeSymbol.asClass
        if (isFreeStyleModule(companionClass))
          fromClass(companionClass)
        else
          List( meth.returnType.typeConstructor)
      }

      s.companion.info.decls.toList.flatMap {
        case x: MethodSymbol if x.isAbstract => fromMethod(x.asMethod)
        case _ => Nil
      }
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

    def run = {
      val algebras   = findAlgebras(weakTypeOf[A].typeSymbol.asClass)
      val coproducts = mkCoproduct(algebras)
      //ugly hack because as String it does not typecheck early which we need for types to be in scope
      val parsed     = coproducts.map( cop => c.parse(cop.toString))
      q"""
      new {
         ..$parsed
      }
      """
    }
    c.Expr[Any](run)
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

    def gen(): Tree = annottees match {
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


    def filterImplicitVars( trees: Template): List[ValDef]  =
      trees.collect { case v: ValDef if v.mods.hasFlag(Flag.DEFERRED) => v }

    object log {
      def err(msg: String): Unit                = c.error(c.enclosingPosition, msg)
      def warn(msg: String): Unit               = c.warning(c.enclosingPosition, msg)
      def info(msg: String): Unit               = c.info(c.enclosingPosition, msg, force = true)
      def rawInfo(name: String, obj: Any): Unit = info(name + " = " + showRaw(obj))
    }

    def mkCompanion( name: TypeName, implicits: List[ValDef] ): ModuleDef = {
      q"""
        object ${name.toTermName} extends Modular {
          val X = materialize.apply(this)
          type Op[A] = X.Op[A]

          class Impl[F[_]](implicit ..$implicits) extends $name[F]

          implicit def instance[F[_]](implicit ..$implicits): $name[F] = new Impl[F]()

          def apply[F[_]](implicit ev: $name[F]): $name[F] = ev
        }
      """
    }

    gen()
  }
}
