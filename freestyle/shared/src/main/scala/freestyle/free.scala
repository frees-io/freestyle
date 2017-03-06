package freestyle

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

@compileTimeOnly("enable macro paradise to expand @free macro annotations")
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
      case _ =>
        fail(
          s"Invalid @free usage, only traits and abstract classes without companions are supported")
    }

    def genModule(cls: ClassDef) = {
      val userTrait @ ClassDef(clsMods, clsName, clsParams, clsTemplate) = cls.duplicate
      if (!clsMods.hasFlag(Flag.TRAIT | Flag.ABSTRACT))
        fail(s"@free requires trait or abstract class")
      mkCompanion(clsName.toTermName, clsParams, userTrait)
    }

    def smartCtorNamedADT(smartCtorName: TypeName) =
      TypeName(smartCtorName.encodedName.toString.capitalize + "OP")

    def mkAdtRoot(name: TypeName) =
      q"sealed trait ${name}[A] extends Product with Serializable"

    def mkAdtLeaves(
        userTrait: ClassDef,
        requestDefs: List[DefDef],
        rootName: TypeName): List[ClassDef] = {

      def mkRequestClass(sc: DefDef): ClassDef = {
        val retType = sc.tpt.asInstanceOf[AppliedTypeTree].args.last
        val tparams = userTrait.tparams.tail ++ sc.tparams
        val args    = sc.vparamss.flatten //.filter(v => !v.mods.hasFlag(Flag.IMPLICIT))
        //TODO user trait type params need to be added to the case class leave generated because its args may contain them
        val className = smartCtorNamedADT(sc.name.toTypeName)
        args match {
          case Nil =>
            q"""case class $className[..${tparams}]() extends $rootName[$retType] """
          case _ =>
            q"""case class $className[..${tparams}](..$args) extends $rootName[$retType]"""
        }
      }
      requestDefs.map( sc => mkRequestClass(sc))
    }

    def mkSmartCtorsImpls(
        typeArgs: List[TypeName],
        adtRootName: TypeName,
        requestDefs: List[DefDef],
        requestClasses: List[ImplDef]): List[DefDef] = {
      for {
        (sc, adtLeaf) <- requestDefs.zip(requestClasses)
        cpType <- typeArgs.headOption.toList
        injTpeArgs = adtRootName :: cpType :: Nil
        ctor <- adtLeaf find {
          case DefDef(_, TermName("<init>"), _, _, _, _) => true
          case _                                         => false
        }
        args = sc.vparamss.flatten.collect {
          case v /*if !v.mods.hasFlag(Flag.IMPLICIT)*/ => v.name
        }
        companionApply = adtLeaf match {
          case c: ClassDef => q"${adtLeaf.name.toTermName}[..${c.tparams.map(_.name)}](..$args)"
          case _           => q"new ${adtLeaf.name.toTypeName}"
        }
        AppliedTypeTree(tpt, _) = sc.tpt
        impl = tpt match {
          case Ident(TypeName(tp)) if tp.endsWith("FreeS") =>
            q"""
              freestyle.FreeS.liftPar(freestyle.FreeS.inject[..$injTpeArgs]($companionApply))
             """
          case Select(Ident(TermName(term)), TypeName(tp)) if tp.endsWith("Par") =>
            q"freestyle.FreeS.inject[..$injTpeArgs]($companionApply)"
          case _ =>
            fail(s"unknown abstract type found in @free container: $tpt : raw: ${showRaw(tpt)}")
        }
      } yield q"override def ${sc.name}[..${sc.tparams}](...${sc.vparamss}): ${sc.tpt} = $impl"
    }

    def mkSmartCtorsClassImpls(
        userTrait: ClassDef,
        parentName: TypeName,
        adtRootName: TypeName,
        parentTypeArgs: List[TypeName],
        smartCtorsImpls: List[DefDef]): ClassDef = {
      val implName    = TypeName(parentName.decodedName.toString + "_default_impl")
      val injTpeArgs  = adtRootName +: parentTypeArgs
      val firstTParam = userTrait.tparams.head
      val impl        = q"""
       class $implName[..${userTrait.tparams}](implicit I: cats.free.Inject[T, ${firstTParam.name}])
          extends $parentName[..${userTrait.tparams.map(_.name)}] {
            ..$smartCtorsImpls
          }
      """
      impl
    }

    def mkCompanionDefaultInstance(
        userTrait: ClassDef,
        smartCtorsImpl: ClassDef): DefDef = {
      val firstTParam = userTrait.tparams.head
      q"implicit def defaultInstance[..${userTrait.tparams}](implicit I: cats.free.Inject[T, ${firstTParam.name}]): ${userTrait.name}[..${userTrait.tparams
        .map(_.name)}] = new ${smartCtorsImpl.name}[..${userTrait.tparams.map(_.name)}]"
    }

    def mkAdtType(adtRootName: TypeName): Tree =
      q"type T[A] = $adtRootName[A]"

    def mkDefaultFunctionK(impls: List[(DefDef, ImplDef, DefDef)]): Match = {
      val functorSteps = for {
        impl <- impls
        (sc, adtLeaf, forwarder) = impl
        wildcardsArgsTpl = sc.vparamss.flatten.collect {
          case v /*if !v.mods.hasFlag(Flag.IMPLICIT)*/ => (q"l.${v.name}", pq"_")
        }
        pattern = pq"l @ ${adtLeaf.name.toTermName}(..${wildcardsArgsTpl.map(_._2)})"
        matchCase = wildcardsArgsTpl match {
          case Nil => cq"$pattern => ${forwarder.name}"
          case _   => cq"$pattern => ${forwarder.name}(..${wildcardsArgsTpl.map(_._1)})"
        }
      } yield matchCase
      q"fa match {case ..$functorSteps}"
    }

    def mkAbstractInterpreter(
        userTrait: ClassDef,
        requestDefs: List[DefDef],
        requestClasses: List[ImplDef]): ClassDef = {
      val firstTParam = userTrait.tparams.head
      val impls: List[(DefDef, ImplDef, DefDef)] = for {
        (sc, adtLeaf) <- requestDefs zip requestClasses
        implName                                    = TermName(sc.name.toTermName.encodedName.toString + "Impl")
        DefDef(_, _, _, _, tpe: AppliedTypeTree, _) = sc
        retType <- tpe.args.lastOption.toList
        params = sc.vparamss.flatten
      } yield
        (sc, adtLeaf, params match {
          case Nil => q"def $implName[..${sc.tparams}]: ${firstTParam.name}[$retType]"
          case _   => q"def $implName[..${sc.tparams}](..$params): ${firstTParam.name}[$retType]"
        })
      val abstractImpls = impls map (_._3)
      val matchCases    = mkDefaultFunctionK(impls)
      q"""abstract class Interpreter[..${userTrait.tparams}] extends cats.arrow.FunctionK[T, ${firstTParam.name}] {
            ..$abstractImpls
            override def apply[A](fa: T[A]): ${firstTParam.name}[A] = $matchCases
          }
      """
    }

    def getRequestDefs(effectTrait: ClassDef): List[DefDef] =
      effectTrait.impl.filter {
        case q"$mods def $name[..$tparams](...$paramss): FreeS[..$args]"     => true
        case q"$mods def $name[..$tparams](...$paramss): FreeS.Par[..$args]" => true
        case _ => false
      }.map(_.asInstanceOf[DefDef])

    def mkCompanion(
        name: TermName,
        clsParams: List[TypeDef],
        userTrait: ClassDef
    ) = {
      val requestDefs: List[DefDef] = getRequestDefs(userTrait)

      val adtRootName     = smartCtorNamedADT(name.toTypeName)
      val requestTrait    = mkAdtRoot(adtRootName)
      val requestClasses  = mkAdtLeaves(userTrait, requestDefs, adtRootName) 
      val requestTyAlias  = mkAdtType(adtRootName)
      val scAdtPairs      = requestDefs.zip(requestClasses)
      val cpTypes         = getTypeParams(clsParams)

      val smartCtorsImpls = mkSmartCtorsImpls(cpTypes, adtRootName, requestDefs, requestClasses)
      val smartCtorsClassImpl =
        mkSmartCtorsClassImpls(userTrait, name.toTypeName, adtRootName, cpTypes, smartCtorsImpls)
      val implicitInstance =
        mkCompanionDefaultInstance(userTrait, smartCtorsClassImpl)
      val abstractInterpreter = mkAbstractInterpreter(userTrait, requestDefs, requestClasses)
      val injectInstance =
        q"implicit def injectInstance[F[_]](implicit I: cats.free.Inject[T, F]): cats.free.Inject[T, F] = I"
      val result = q"""
        $userTrait
        object $name {
          $requestTrait
          ..$requestClasses
          $requestTyAlias
          $smartCtorsClassImpl
          $implicitInstance
          def apply[..${userTrait.tparams}](implicit c: ${userTrait.name}[..${userTrait.tparams
        .map(_.name)}]): ${userTrait.name}[..${userTrait.tparams.map(_.name)}] = c
          $abstractInterpreter
        }
      """
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
