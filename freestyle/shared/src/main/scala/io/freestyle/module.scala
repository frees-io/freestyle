package io.freestyle

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

    def isFreeStyleModule(cs: Symbol): Boolean = {
      if (cs.isClass) {
        cs.asClass.baseClasses.collectFirst {
          case x: ClassSymbol if x.name == TypeName("Modular") => x
        }.isDefined
      } else false
    }

    def extractCoproductSymbol(cs: Symbol): List[TermName] = {
      cs match {
        case c: ClassSymbol =>
          //println("cs is a class symbol: " + cs.name  + " wih decls: " + c.companion.info.decls.map(_.name))
          if (!isFreeStyleModule(c)) {
            TermName(c.fullName) :: Nil
            //println("candidate T: " + cs)
            //c.info.member(TypeName("T")) :: Nil
          } else {
            c.companion.info.decls.toList.flatMap(x => extractCoproductSymbol(x))
          }
        case m: MethodSymbol =>
          //println("found candidate nested method with return type: " + m.returnType.companion)
          extractCoproductSymbol(m.returnType.companion.typeSymbol.asClass)
        case _ =>
          Nil
      }
    }

    def findAlgebras(s: Type): List[TermName] = {
      def loop(l: List[Symbol], acc: List[TermName]): List[TermName] = {
        l match {
          case Nil => acc
          case h :: t =>
            val companionClass = h.asMethod.returnType.companion.typeSymbol.asClass
            val coproducts     = extractCoproductSymbol(companionClass)
            loop(t, acc ++ coproducts)
        }
      }
      loop(s.decls.toList.filter(_.isAbstract), Nil) //.map(_.asType.typeSignature.resultType.typeSymbol)
    }

    def cp(n: TermName) =
      Select(Ident(n), TypeName("T"))

    def mkModuleCoproduct(algebras: List[TermName]): List[String] = { //ugly hack because as String it does not typecheck early which we need for types to be in scope
      val result = algebras match {
        case List(el) =>
          (0, q"type T[A] = cats.data.Coproduct[$el.T, $el.T, A]") :: Nil //TODO this won't work but we need a solution to single service modules
        case l @ _ :: _ =>
          l.scanLeft[(Int, AnyRef), List[(Int, AnyRef)]]((0, q"")) {
            case ((pos, acc), el) =>
              val cpName = if (pos + 1 != l.size) TypeName(s"C$pos") else TypeName("T")
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
      //println("parsed: \n" + parsed)
      val tree = q"""
      new {
         ..$parsed
      }
      """
      //println(tree)
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
      case List(Expr(cls: ClassDef)) => genModule(cls)
      case _ =>
        fail(
          s"Invalid @module usage, only traits and abstract classes without companions are supported")
    }

    def genModule(cls: ClassDef) = {
      val userTrait @ ClassDef(clsMods, clsName, clsParams, clsTemplate) = cls.duplicate
      if (!clsMods.hasFlag(Flag.TRAIT | Flag.ABSTRACT))
        fail(s"@free requires trait or abstract class")
      mkCompanion(clsName.toTermName, clsTemplate.filter {
        case _: ValDef => true
        case _         => false
      }, clsParams, userTrait)
    }

    def mkImplicitArgs(clsRestBody: List[Tree]): List[ValDef] =
      clsRestBody collect { case m @ ValDef(mods, _, _, _) if mods.hasFlag(Flag.DEFERRED) => m }

    def mkModuleClassImpls(parentName: TypeName, implicitArgs: List[ValDef]): ClassDef = {
      val implName = TypeName(parentName.decodedName.toString + "_default_impl")
      val impl     = q"""
       class $implName[F[_]](implicit ..$implicitArgs)
          extends $parentName[F]
      """
      impl
    }

    def mkCompanionApply(
        userTrait: ClassDef,
        classImpl: ClassDef,
        implicitArgs: List[ValDef]): DefDef = {
      val ev        = freshTermName("instance")
      val implicits = q"val $ev: ${userTrait.name.toTypeName}[F]"
      q"def apply[F[_]](implicit ..$implicits): ${userTrait.name.toTypeName}[F] = $ev"
    }

    def mkCompanionDefaultInstance(
        userTrait: ClassDef,
        classImpl: ClassDef,
        implicitArgs: List[ValDef]): DefDef = {
      val instanceName = freshTermName(userTrait.name.decodedName.toString)
      val implicits    = implicitArgs //  :+ q"val I: Inject[T, F]"
      q"implicit def $instanceName[F[_]](implicit ..$implicits): ${userTrait.name.toTypeName}[F] = new ${classImpl.name}[F]()"
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
          case h :: t =>
            loop(t, acc)
        }
      loop(topLevel.map(i => TermName(i.toString)), Nil)
    }

    def mkImplicitsTrait(userTrait: ClassDef, implicitArgs: List[ValDef]): ClassDef = {
      val instanceName = freshTermName(userTrait.name.decodedName.toString + "DefaultInstance")
      val implicits    = implicitArgs //:+ q"val I: Inject[T, F]"
      val rawTypeDefs  = implicitArgs.flatMap(_.tpt.children.headOption)
      val parents = rawTypeDefs.map { n =>
        Select(Ident(TermName(n.toString)), TypeName("Implicits"))
      }
      q"""
        trait Implicits extends ..${parents} {
           implicit def $instanceName[F[_]](implicit ..$implicits): ${userTrait.name}[F] = defaultInstance[F]
        }
      """
    }

    def mkModuleCoproduct(implicitArgs: List[ValDef]): List[String] = {
      val rawTypeDefs = implicitArgs.flatMap(_.tpt.children.headOption)
      val result = rawTypeDefs match {
        case List(el) =>
          (0, q"type T[A] = Coproduct[${TermName(el.toString)}.T, cats.Id, A]") :: Nil
        case _ =>
          rawTypeDefs.scanLeft[(Int, AnyRef), List[(Int, AnyRef)]]((0, q"")) {
            case ((pos, acc), el) =>
              val z      = TermName(el.toString)
              val cpName = if (pos + 1 != rawTypeDefs.size) TypeName(s"C$pos") else TypeName("T")
              val newTree = (acc, z) match {
                case (q"", r: TermName) => r
                case (l: TermName, r: TermName) =>
                  q"type $cpName[A] = cats.data.Coproduct[$l.T, $r.T, A]"
                case (l: TypeDef, r: TermName) =>
                  q"type $cpName[A] = cats.data.Coproduct[$r.T, ${l.name}, A]"
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

    def mkCompanion(
        name: TermName,
        clsRestBody: List[Tree],
        clsParams: List[TypeDef],
        userTrait: ClassDef
    ) = {
      val implicitArgs     = mkImplicitArgs(clsRestBody)
      val moduleCoproduct  = mkModuleCoproduct(implicitArgs).map(c.parse(_))
      val moduleClassImpl  = mkModuleClassImpls(name.toTypeName, implicitArgs)
      val implicitInstance = mkCompanionDefaultInstance(userTrait, moduleClassImpl, implicitArgs)
      //val implicitsTrait = mkImplicitsTrait(userTrait, implicitArgs)
      val rawTypeDefs = implicitArgs.flatMap(_.tpt.children.headOption)
      //val parents = rawTypeDefs.map { n => Select(Ident(TermName(n.toString)), TypeName("Implicits")) }
      val companionApply = mkCompanionApply(userTrait, moduleClassImpl, implicitArgs)
      val typeMaterializers = moduleCoproduct match {
        case Nil => q"type T[A] = Nothing" :: Nil
        case _   => q"val X = io.freestyle.materialize.apply(this)" :: q"type T[A] = X.T[A]" :: Nil
      }
      val result = q"""
        $userTrait
        $moduleClassImpl
        object $name extends io.freestyle.Modular {
          $implicitInstance
          $companionApply
          ..$typeMaterializers
        }
      """
      println(result)
      result
    }

    gen()
  }
}
