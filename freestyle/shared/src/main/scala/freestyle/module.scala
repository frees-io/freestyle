package freestyle

import cats.instances.tuple._
import cats.syntax.functor._

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{currentMirror => cm}

trait Modular

object materialize {

  def apply[A](a: A): Any = macro materializeImpl[A]

  def materializeImpl[A](c: whitebox.Context)(a: c.Expr[A])(
      implicit f: c.WeakTypeTag[A]): c.Expr[Any] = {
    import c.universe._

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    object log {
      def err(msg: String): Unit                = c.error(c.enclosingPosition, msg)
      def warn(msg: String): Unit               = c.warning(c.enclosingPosition, msg)
      def info(msg: String): Unit               = c.info(c.enclosingPosition, msg, force = true)
      def rawInfo(name: String, obj: Any): Unit = info(name + " = " + showRaw(obj))
    }

    def findAlgebras(s: Type): List[TermName] = {

      def isFreeStyleModule(cs: ClassSymbol): Boolean =
        cs.baseClasses.exists {
          case x: ClassSymbol => x.name == TypeName("Modular")
        }

      def fromClass(c: ClassSymbol, t: Type) : List[TermName] =
        if (isFreeStyleModule(c)) {
          c.companion.info.decls.toList.flatMap {
            case x: MethodSymbol => fromMethod(x.asMethod)
            case _ => Nil
          }
        } else TermName(t.toString) :: Nil

      def fromMethod(m: MethodSymbol): List[TermName] = {
        val companionClass    = m.returnType.companion.typeSymbol.asClass
        val dependentTypeCtor = m.returnType.typeConstructor
        fromClass(companionClass, dependentTypeCtor)
      }

      s.decls.toList.filter(_.isAbstract).flatMap(sym => fromMethod(sym.asMethod))
    }

    def cp(n: TermName) =
      Select(Ident(n), TypeName("Op"))

    def mkModuleCoproduct(algebras: List[TermName]): List[String] = { //ugly hack because as String it does not typecheck early which we need for types to be in scope
      val result = algebras match {
        case List(el) =>
          (0, q"type Op[A] = cats.data.Coproduct[$el.Op, Nothing, A]") :: Nil //TODO this won't work but we need a solution to single service modules
        case l @ _ :: _ =>
          l.scanLeft[(Int, AnyRef), List[(Int, AnyRef)]]((0, q"")) {
            case ((pos, acc), el) =>
              val cpName = if (pos + 1 != l.size) TypeName(s"C$pos") else TypeName("Op")
              val newTree = (acc, el) match {
                case (q"", r: TermName) => r
                case (l: TermName, r: TermName) =>
                  q"type $cpName[A] = cats.data.Coproduct[${cp(l)}, ${cp(r)}, A]"
                case (l: TypeDef, r: TermName) =>
                  q"type $cpName[A] = cats.data.Coproduct[${cp(r)}, ${l.name}, A]"
                case x =>
                  fail(
                    s"found unexpected case building Coproduct: $x with types ${x.map(_.getClass)}")
              }
              (pos + 1, newTree)
          }

      }
      result collect {
        case (_, x: TypeDef) => x.toString
      }
    }

    def run = {
      val root             = weakTypeOf[A].typeSymbol.asClass.companion.info
      val algebras         = findAlgebras(root)
      val moduleCoproducts = mkModuleCoproduct(algebras)
      val parsed           = moduleCoproducts.map(c.parse(_))
      val tree             = q"""
      new {
         ..$parsed
      }
      """
      c.typecheck(tree.duplicate, c.TYPEmode)
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
        if (cls.mods.hasFlag(Flag.TRAIT | Flag.ABSTRACT))
          genModule(cls)
        else
          fail(s"@free requires trait or abstract class")
      case _ =>
        fail(
          s"Invalid @module usage, only traits and abstract classes without companions are supported")
    }

    def genModule(cls: ClassDef) = {
      val userTrait @ ClassDef(clsMods, name, _, clsTemplate) = cls.duplicate

      val valDefs: List[ValDef] = clsTemplate.filter {
        case _: ValDef => true
        case _         => false
      }.map(_.asInstanceOf[ValDef])
      val implicitArgs: List[ValDef] = valDefs.filter(_.mods.hasFlag(Flag.DEFERRED))

      val moduleClass = mkModuleClass(name, implicitArgs)
      val companion = mkCompanion(name, implicitArgs)

      q"""
        $userTrait
        $moduleClass
        $companion
      """
    }

    def mkModuleClassName( parentName: TypeName) : TypeName =
      TypeName(parentName.decodedName.toString + "_default_impl")

    def mkModuleClass(parentName: TypeName, implicits: List[ValDef]): ClassDef = {
      val className = mkModuleClassName(parentName)

      q"class $className[F[_]](implicit ..$implicits) extends $parentName[F] "
    }

    def packageName(sym: Symbol) = {
      def enclosingPackage(sym: Symbol): Symbol = {
        if (sym == NoSymbol) NoSymbol
        else if (sym.isPackage) sym
        else enclosingPackage(sym.owner)
      }
      val pkg = enclosingPackage(sym)
      if (pkg == cm.EmptyPackageClass) ""
      else pkg.fullName
    }

    object log {
      def err(msg: String): Unit                = c.error(c.enclosingPosition, msg)
      def warn(msg: String): Unit               = c.warning(c.enclosingPosition, msg)
      def info(msg: String): Unit               = c.info(c.enclosingPosition, msg, force = true)
      def rawInfo(name: String, obj: Any): Unit = info(name + " = " + showRaw(obj))
    }

    def extractCoproductTypes(topLevel: List[Tree]): List[TypeName] = {
      def loop(remaining: List[TermName], acc: List[TypeName]): List[TypeName] =
        remaining match {
          case Nil => acc
          case h :: t => loop(t, acc)
        }
      loop(topLevel.map(i => TermName(i.toString)), Nil)
    }

    def mkImplicitsTrait(name: TypeName, implicitArgs: List[ValDef]): ClassDef = {
      val instanceName = freshTermName(name.decodedName.toString + "DefaultInstance")
      val implicits    = implicitArgs //:+ q"val I: Inject[T, F]"
      val rawTypeDefs  = implicitArgs.flatMap(_.tpt.children.headOption)
      val parents = rawTypeDefs.map { n =>
        Select(Ident(TermName(n.toString)), TypeName("Implicits"))
      }
      q"""
        trait Implicits extends ..${parents} {
           implicit def $instanceName[F[_]](implicit ..$implicits): $name[F] = defaultInstance[F]
        }
      """
    }

    def mkModuleCoproduct(implicits: List[ValDef]): List[String] = {
      val rawTypeDefs = implicits.flatMap(_.tpt.children.headOption)
      val result = rawTypeDefs match {
        case List(el) =>
          (0, q"type Op[A] = Coproduct[${TermName(el.toString)}.Op, cats.Id, A]") :: Nil
        case _ =>
          rawTypeDefs.scanLeft[(Int, AnyRef), List[(Int, AnyRef)]]((0, q"")) {
            case ((pos, acc), el) =>
              val z      = TermName(el.toString)
              val cpName = if (pos + 1 != rawTypeDefs.size) TypeName(s"C$pos") else TypeName("Op")
              val newTree = (acc, z) match {
                case (q"", r: TermName) => r
                case (l: TermName, r: TermName) =>
                  q"type $cpName[A] = cats.data.Coproduct[$l.Op, $r.Op, A]"
                case (l: TypeDef, r: TermName) =>
                  q"type $cpName[A] = cats.data.Coproduct[$r.Op, ${l.name}, A]"
                case x =>
                  fail(
                    s"found unexpected case building Coproduct: $x with types ${x.map(_.getClass)}")
              }
              (pos + 1, newTree)
          }
      }

      result collect {
        case (_, x: TypeDef) => x.toString
      }
    }

    def mkDefaultInstance( name: TypeName, implicitArgs: List[ValDef]): DefDef = {
      val className = mkModuleClassName(name)
      val instanceName = freshTermName(name.decodedName.toString)
      val implicits    = implicitArgs //  :+ q"val I: Inject[T, F]"
      q"implicit def $instanceName[F[_]](implicit ..$implicits): $name[F] = new $className[F]()"
    }

    def mkCompanion( name: TypeName, implicitArgs: List[ValDef] ): ModuleDef = {

      val implicitInstance = mkDefaultInstance(name, implicitArgs)
      val typeMaterializers = mkModuleCoproduct(implicitArgs).map(c.parse(_)) match {
        case Nil => q"type Op[A] = Nothing" :: Nil
        case _   => q"val X = materialize.apply(this)" :: q"type Op[A] = X.Op[A]" :: Nil
      }
      q"""
        object ${name.toTermName} extends Modular {
          ..$typeMaterializers

          $implicitInstance

          def apply[F[_]](implicit ev: $name[F]): $name[F] = ev
        }
      """
    }

    gen()
  }
}
