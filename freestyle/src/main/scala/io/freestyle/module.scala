package io.freestyle

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import cats._
import cats.free._
import cats.data._
import cats.arrow._
import cats.implicits._

class module extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro module.impl
}

object module {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.universe.Tree = {
    import c.universe._
    import internal.reificationSupport._

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    def gen(): Tree = annottees match {
      case List(Expr(cls: ClassDef)) => genModule(cls)
      case _ => fail(s"Invalid @module usage, only traits and abstract classes without companions are supported")
    }

    def genModule(cls: ClassDef) = {
      val userTrait @ ClassDef(clsMods, clsName, clsParams, clsTemplate) = cls
      if (!clsMods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) fail(s"@free requires trait or abstract class")
      mkCompanion(clsName.toTermName, clsTemplate.filter {
        case _: ValDef => true
        case _ => false
      }, clsParams, userTrait)
    }

    def mkImplicitArgs(clsRestBody: List[Tree]): List[ValDef] = {
      clsRestBody collect { case m: ValDef => m }
    }

    def mkModuleClassImpls(parentName: TypeName, implicitArgs: List[ValDef]): ClassDef = {
      val implName = TypeName(parentName.decodedName.toString + "_default_impl")
      val impl = q"""
       class $implName[F[_]](implicit ..$implicitArgs)
          extends $parentName[F]
      """
      impl
    }

    def mkCompanionApply(userTrait: ClassDef, classImpl: ClassDef, implicitArgs: List[ValDef]): DefDef = {
      val implicits = q"val instance: ${userTrait.name.toTypeName}[F]" :: Nil
      q"def apply[F[_]]()(implicit ..$implicits): ${userTrait.name.toTypeName}[F] = instance"
    }

    def mkCompanionDefaultInstance(userTrait: ClassDef, classImpl: ClassDef, implicitArgs: List[ValDef]): DefDef = {
      val instanceName = freshTermName(userTrait.name.decodedName.toString)
      q"implicit def $instanceName[F[_]](implicit ..$implicitArgs): ${userTrait.name.toTypeName}[F] = new ${classImpl.name}[F]()"
    }

    def mkModuleCoproduct(implicitArgs: List[ValDef]): List[Tree] = {
      val rawTypeDefs = implicitArgs.flatMap(_.tpt.children.headOption)
      val result = rawTypeDefs match {
        case List(el) => (0, q"type T[A] = Coproduct[${TermName(el.toString)}.T, cats.Id, A]") :: Nil
        case _ => rawTypeDefs.scanLeft[(Int, AnyRef), List[(Int, AnyRef)]]((0, q"")) {
          case ((pos, acc), el) =>
            val z = TermName(el.toString)
            val cpName = if (pos + 1 != rawTypeDefs.size) TypeName(s"C$pos") else TypeName("T")
            val newTree = (acc, z) match {
              case (q"", r: TermName) => r
              case (l: TermName, r: TermName) => q"type $cpName[A] = Coproduct[$l.T, $r.T, A]"
              case (l: TypeDef, r: TermName) => q"type $cpName[A] = Coproduct[$r.T, ${l.name}, A]"
              case x => fail(s"found unexpected case building Coproduct: $x with types ${x.map(_.getClass)}")
            }
            (pos + 1, newTree)
        }

      }
      result collect {
        case (_, x: TypeDef) => x
      }
    }

    def mkModuleInterpreter(implicitArgs: List[ValDef]): List[Tree] = {
      val rawTypeDefs = implicitArgs.flatMap(_.tpt.children.headOption)
      val result = rawTypeDefs.scanLeft[(Int, AnyRef), List[(Int, AnyRef)]]((0, q"")) {
        case ((pos, acc), el) =>
          val z = TermName(el.toString)
          val cpName = if (pos + 1 != rawTypeDefs.size) TypeName(s"C$pos") else TypeName("T")
          val iName = freshTermName("interpreter")
          val newTree = (acc, z) match {
            case (q"", r: TermName) => r
            case (l: TermName, r: TermName) =>
              val lEv = freshTermName("l")
              val rEv = freshTermName("r")
              val implicitsFKs = q"val $lEv: cats.arrow.FunctionK[$l.T, M]" :: q"val $rEv: cats.arrow.FunctionK[$r.T, M]" :: Nil
              q"implicit def $iName[M[_]](implicit ..$implicitsFKs): cats.arrow.FunctionK[$cpName, M] = $lEv.or($rEv)"
            case (l: DefDef, r: TermName) =>
              val rEv = freshTermName("r")
              val evs = l.vparamss.flatMap(_ :+ q"val $rEv: cats.arrow.FunctionK[$r.T, M]")
              q"implicit def $iName[M[_]](implicit ..$evs): cats.arrow.FunctionK[$cpName, M] = $rEv.or(${l.name}[M])"
            case x => fail(s"found unexpected case building Interpreter: $x with types ${x.map(_.getClass)}")
          }
          (pos + 1, newTree)
      }
      result collect {
        case (_, x: DefDef) => x
      }
    }

    def mkImplicitsTrait(prefix: TermName, implicitArgs: List[ValDef], contents: List[Tree]): ClassDef = {
      val rawTypeDefs = implicitArgs.flatMap(_.tpt.children.headOption)
      val parents = rawTypeDefs.map { n => Select(Ident(TermName(n.toString)), TypeName("Implicits")) }
      val traitName = TypeName(prefix.decodedName.toString + "Implicits")
      q"""
        trait $traitName extends ..$parents {
          ..$contents
        }
      """
    }

    def mkCompanion(
      name: TermName,
      clsRestBody: List[Tree],
      clsParams: List[TypeDef],
      userTrait: ClassDef
    ) = {
      val implicitArgs = mkImplicitArgs(clsRestBody)
      val moduleClassImpl = mkModuleClassImpls(name.toTypeName, implicitArgs)
      val companionApply = mkCompanionApply(userTrait, moduleClassImpl, implicitArgs)
      val defaultInstance = mkCompanionDefaultInstance(userTrait, moduleClassImpl, implicitArgs)
      val moduleCoproduct = mkModuleCoproduct(implicitArgs)
      val moduleInterpreter = mkModuleInterpreter(implicitArgs)
      val implicitsTrait = mkImplicitsTrait(name, implicitArgs, moduleClassImpl :: defaultInstance :: Nil ++ moduleCoproduct ++ moduleInterpreter)
      val result = q"""
        $userTrait
        object $name extends ${implicitsTrait.name} {
          $companionApply
          type Implicits = ${implicitsTrait.name}
        }
        $implicitsTrait
      """
      println(result)
      result
    }

    gen()
  }
}
