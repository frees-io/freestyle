package io.freestyle

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import cats._
import cats.free._
import cats.data._
import cats.arrow._
import cats.implicits._
import scala.annotation._

object syntax {

  implicit class FreeSyntax[F[_], A](fa: Free[F, A]) {
    def exec[M[_]: Monad: RecursiveTailRecM](implicit interpreter: FunctionK[F, M]): M[A] =
      fa.foldMap(interpreter)
  }

  implicit def interpretCoproduct[F[_], G[_], M[_]](implicit fm: FunctionK[F,M], gm: FunctionK[G, M]): FunctionK[Coproduct[F, G, ?], M] =
    fm or gm

}

@compileTimeOnly("enable macro paradise to expand @module macro annotations")
class free extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro free.impl
}

object free {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.universe.Tree = {
    import c.universe._
    import internal.reificationSupport._

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    def gen(): Tree = annottees match {
      case List(Expr(cls: ClassDef)) => genModule(cls)
      case _ => fail(s"Invalid @free usage, only traits and abstract classes without companions are supported")
    }

    def genModule(cls: ClassDef) = {
      val userTrait @ ClassDef(clsMods, clsName, clsParams, clsTemplate) = cls
      if (!clsMods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) fail(s"@free requires trait or abstract class")
      mkCompanion(clsName.toTermName, clsTemplate.filter {
        case _: DefDef => true
        case _ => false
      }, clsParams, userTrait)
    }

    def smartCtorNamedADT(smartCtorName: TypeName) =
      TypeName(smartCtorName.encodedName.toString + "OP")

    def mkAdtRoot(name: TypeName) = {
      q"sealed trait ${name}[A] extends Product with Serializable"
    }

    def mkAdtLeaves(clsRestBody: List[Tree], rootName: TypeName): List[(DefDef, ClassDef)] = {
      for {
        method <- clsRestBody filter {
          case q"$mods def $name[..$tparams](...$paramss): Free[..$args]" => true
          case _ => false
        }
        sc @ DefDef(_, _, _, _, tpe: AppliedTypeTree, _) = method
        retType <- tpe.args.lastOption.toList
      } yield (
        sc,
        q"""final case class ${smartCtorNamedADT(sc.name.toTypeName)}(...${sc.vparamss})
            extends $rootName[$retType]"""
      )
    }

    def mkSmartCtorsImpls(typeArgs: List[TypeName], adtRootName: TypeName, scAdtPairs: List[(DefDef, ClassDef)]): List[DefDef] = {
      for {
        scAdtPair <- scAdtPairs
        (sc, adtLeaf) = scAdtPair
        cpType <- typeArgs.headOption.toList
        injTpeArgs = adtRootName :: cpType :: Nil
        ctor <- adtLeaf find {
          case DefDef(_, TermName("<init>"), _, _, _, _) => true
          case _ => false
        }
        args = sc.vparamss.flatten.map(_.name)
        companionApply = q"${adtLeaf.name.toTermName}(..$args)"
        impl = q"Free.inject[..$injTpeArgs]($companionApply)"
      } yield q"def ${sc.name}[..${sc.tparams}](...${sc.vparamss}): ${sc.tpt} = $impl"
    }

    def mkSmartCtorsClassImpls(parentName: TypeName, adtRootName: TypeName, parentTypeArgs: List[TypeName], smartCtorsImpls: List[DefDef]): ClassDef = {
      val implName = TypeName(parentName.decodedName.toString + "_default_impl")
      val injTpeArgs = adtRootName +: parentTypeArgs
      val impl = q"""
       class $implName[F[_]](implicit I: cats.free.Inject[T, F])
          extends $parentName[F] {
            ..$smartCtorsImpls
          }
      """
      impl
    }

    def mkCompanionDefaultInstance(userTrait: ClassDef, smartCtorsImpl: ClassDef, adtRootName: TypeName): DefDef = {
      q"implicit def defaultInstance[F[_]](implicit I: cats.free.Inject[T, F]): ${userTrait.name}[F] = new ${smartCtorsImpl.name}[F]"
    }

    def mkAdtType(adtRootName: TypeName): Tree =
      q"type T[A] = $adtRootName[A]"

    def mkDefaultFunctionK(adtRootName: TypeName, impls: List[(DefDef, ClassDef, DefDef)]): Match = {
      val functorSteps = for {
        impl <- impls
        (sc, adtLeaf, forwarder) = impl
        args = sc.vparamss.flatten.map(_.name).map(arg => q"l.$arg")
        pattern = pq"l : ${adtLeaf.name}"
        matchCase = cq"$pattern => ${forwarder.name}(..$args)"
      } yield matchCase
      q"fa match {case ..$functorSteps}"
    }

    def mkAbstractInterpreter(adtRootName: TypeName, scAdtPairs: List[(DefDef, ClassDef)]): ClassDef = {
      val impls: List[(DefDef, ClassDef, DefDef)] = for {
        scAdtPair <- scAdtPairs
        (sc, adtLeaf) = scAdtPair
        implName = TermName(sc.name.toTermName.encodedName.toString + "Impl")
        DefDef(_, _, _, _, tpe: AppliedTypeTree, _) = sc
        retType <- tpe.args.lastOption.toList
      } yield (sc, adtLeaf, q"def $implName(...${sc.vparamss}): M[$retType]")
      val abstractImpls = impls map (_._3)
      val matchCases = mkDefaultFunctionK(adtRootName, impls)
      q"""abstract class Interpreter[M[_]] extends cats.arrow.FunctionK[T, M] {
         ..$abstractImpls
         def apply[A](fa: T[A]): M[A] = $matchCases
       }
       """
    }

    def mkImplicitsTrait(userTrait: ClassDef): ClassDef = {
      val instanceName = freshTermName(userTrait.name.decodedName.toString + "DefaultInstance")
      q"""
        trait Implicits {
           implicit def $instanceName[F[_]](implicit I: Inject[T, F]): ${userTrait.name}[F] = defaultInstance
        }
      """
    }

    def mkCompanion(
      name: TermName,
      clsRestBody: List[Tree],
      clsParams: List[TypeDef],
      userTrait: ClassDef
    ) = {
      val adtRootName = smartCtorNamedADT(name.toTypeName)
      val adtRoot = mkAdtRoot(adtRootName)
      val scAdtPairs = mkAdtLeaves(clsRestBody, adtRootName)
      val adtLeaves = scAdtPairs map (_._2)
      val cpTypes = getTypeParams(clsParams)
      val smartCtorsImpls = mkSmartCtorsImpls(cpTypes, adtRootName, scAdtPairs)
      val smartCtorsClassImpl = mkSmartCtorsClassImpls(name.toTypeName, adtRootName, cpTypes, smartCtorsImpls)
      val implicitInstance = mkCompanionDefaultInstance(userTrait, smartCtorsClassImpl, adtRootName)
      val adtType = mkAdtType(adtRootName)
      val abstractInterpreter = mkAbstractInterpreter(adtRootName, scAdtPairs)
      //val implicitsTrait = mkImplicitsTrait(userTrait)
      val injectInstance = q"implicit def injectInstance[F[_]](implicit I: Inject[T, F]): Inject[T, F] = I"
      val result = q"""
        $userTrait
        object $name {
          $adtRoot
          ..$adtLeaves
          $adtType
          $smartCtorsClassImpl
          $implicitInstance
          def apply[F[_]](implicit I: Inject[T, F], c: ${userTrait.name}[F]): ${userTrait.name}[F] = c
          $abstractInterpreter
        }
      """
      //println(result)
      result
    }

    def getTypeParams(params: List[TypeDef]): List[TypeName] = {
      params.collect {
        case t: TypeDef if t.mods.hasFlag(Flag.PARAM) => t.name
      }
    }

    gen()
  }
}
 
