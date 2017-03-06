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
      case List(Expr(cls: ClassDef)) =>
        val effectTrait @ ClassDef(clsMods, _, _, _) = cls.duplicate
        if (clsMods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) {
          val effectObject = mkEffectObject(effectTrait)
          q"""
            $effectTrait
            $effectObject
          """
        } else
          fail(s"@free requires trait or abstract class")
      case _ =>
        fail(
          s"Invalid @free usage, only traits and abstract classes without companions are supported")
    }

    def toRequestAdtName(requestDefName: TypeName) =
      TypeName(requestDefName.encodedName.toString.capitalize + "OP")

    def mkRequestTrait(requestTypeName: TypeName) =
      q"sealed trait $requestTypeName[A] extends Product with Serializable"

      def mkRequestClass(effectTyParams: List[TypeDef], requestType: TypeName, sc: DefDef): ClassDef = {
        val retType = sc.tpt.asInstanceOf[AppliedTypeTree].args.last
        val tparams = effectTyParams.tail ++ sc.tparams
        val args    = sc.vparamss.flatten //.filter(v => !v.mods.hasFlag(Flag.IMPLICIT))
        //TODO user trait type params need to be added to the case class leave generated because its args may contain them
        val className = toRequestAdtName(sc.name.toTypeName)
        args match {
          case Nil =>
            q"""case class $className[..$tparams]() extends $requestType[$retType] """
          case _ =>
            q"""case class $className[..$tparams](..$args) extends $requestType[$retType]"""
        }
      }

    def mkSmartCtor(
      effectTrait: ClassDef,
      effectTyParams: List[TypeDef],
      requestType: TypeName,
      requestDefs: List[DefDef],
      requestClasses: List[ClassDef]
    ): ClassDef = {
      // Constraint: cpTypes non empty
      val cpType: TypeName = getTypeParams(effectTyParams).head
      val smartCtorsImpls =
        requestDefs.zip(requestClasses)
          .map(p => mkSmartCtorImpl(requestType, cpType, p._1, p._2) )

      val effectName  = effectTrait.name.toTermName.toTypeName
      val implName    = TypeName( effectName.decodedName.toString + "_default_impl")
      val firstTParam = effectTyParams.head

      q"""
       class $implName[..$effectTyParams](implicit I: cats.free.Inject[T, ${firstTParam.name}])
          extends $effectName[..${effectTyParams.map(_.name)}] {
            ..$smartCtorsImpls
          }
      """
    }

      def mkSmartCtorImpl(
        requestType: TypeName,
        cpType: TypeName,
        requestDef: DefDef,
        requestClass: ImplDef): DefDef = {

        val ctor = requestClass.find {
          case DefDef(_, TermName("<init>"), _, _, _, _) => true
          case _                                         => false
        }.get

        val args = requestDef.vparamss.flatten.collect {
          case v /*if !v.mods.hasFlag(Flag.IMPLICIT)*/ => v.name
        }
        val companionApply = requestClass match {
          case c: ClassDef => q"${requestClass.name.toTermName}[..${c.tparams.map(_.name)}](..$args)"
          case _           => q"new ${requestClass.name.toTypeName}"
        }
        val inject = q"freestyle.FreeS.inject[$requestType, $cpType]($companionApply)"
        val tpt = requestDef.tpt.asInstanceOf[AppliedTypeTree].tpt
        val impl = tpt match {
          case Ident(TypeName(tp)) if tp.endsWith("FreeS") =>
            q"freestyle.FreeS.liftPar($inject)"
          case Select(Ident(TermName(term)), TypeName(tp)) if tp.endsWith("Par") =>
            inject
          case _ =>
            fail(s"unknown abstract type found in @free container: $tpt : raw: ${showRaw(tpt)}")
        }
        q"override def ${requestDef.name}[..${requestDef.tparams}](...${requestDef.vparamss}): ${requestDef.tpt} = $impl"

      }


    def mkCompanionDefaultInstance(
        effectTrait: ClassDef,
        smartCtorsImpl: ClassDef): DefDef = {
      val firstTParam = effectTrait.tparams.head
      q"implicit def defaultInstance[..${effectTrait.tparams}](implicit I: cats.free.Inject[T, ${firstTParam.name}]): ${effectTrait.name}[..${effectTrait.tparams
        .map(_.name)}] = new ${smartCtorsImpl.name}[..${effectTrait.tparams.map(_.name)}]"
    }

    def mkAbstractInterpreter(
        effectTyParams: List[TypeDef],
        requestDefs: List[DefDef],
        requestClasses: List[ImplDef]): ClassDef = {

      val firstTParam = effectTyParams.head

      def mkMatchCase(sc: DefDef, requestClass: ImplDef): CaseDef  = {
        val wildcardsArgsTpl = sc.vparamss.flatten.collect {
          case v /*if !v.mods.hasFlag(Flag.IMPLICIT)*/ => (q"l.${v.name}", pq"_")
        }
        val fields = wildcardsArgsTpl.map(_._1)
        val wildcards = wildcardsArgsTpl.map(_._2)
        val delegate = TermName(sc.name.toTermName.encodedName.toString + "Impl")
        val pattern = pq"l @ ${requestClass.name.toTermName}(..$wildcards)"
        wildcardsArgsTpl match {
          case Nil => cq"$pattern => $delegate"
          case _   => cq"$pattern => $delegate(..$fields)"
        }
      }

      def mkAbstractImpl(sc: DefDef) = {
        val implName = TermName(sc.name.toTermName.encodedName.toString + "Impl")
        // args has at least one element
        val retType = sc.tpt.asInstanceOf[AppliedTypeTree].args.last
        val params = sc.vparamss.flatten
        params match {
          case Nil => q"def $implName[..${sc.tparams}]: ${firstTParam.name}[$retType]"
          case _   => q"def $implName[..${sc.tparams}](..$params): ${firstTParam.name}[$retType]"
        }
      }

      val abstractImpls = requestDefs.map( p => mkAbstractImpl(p) )
      val matchCases = requestDefs.zip(requestClasses).map( p => mkMatchCase(p._1, p._2) )

      q"""abstract class Interpreter[..$effectTyParams] extends cats.arrow.FunctionK[T, ${firstTParam.name}] {
            ..$abstractImpls
            override def apply[A](fa: T[A]): ${firstTParam.name}[A] = fa match {case ..$matchCases}
          }
      """
    }

    def getRequestDefs(effectTrait: ClassDef): List[DefDef] =
      effectTrait.impl.filter {
        case q"$mods def $name[..$tparams](...$paramss): FreeS[..$args]"     => true
        case q"$mods def $name[..$tparams](...$paramss): FreeS.Par[..$args]" => true
        case _ => false
      }.map(_.asInstanceOf[DefDef])

    def mkEffectObject(effectTrait: ClassDef) : ModuleDef= {

      val name: TermName = effectTrait.name.toTermName
      val effectTyParams: List[TypeDef] = effectTrait.tparams

      val requestDefs    : List[DefDef]   = getRequestDefs(effectTrait)
      val requestType    : TypeName       = toRequestAdtName(name.toTypeName)
      val requestTrait   : ClassDef       = mkRequestTrait(requestType)
      val requestClasses : List[ClassDef] = requestDefs.map( sc => mkRequestClass(effectTyParams, requestType, sc))
      val requestTyAlias = q"type T[A] = $requestType[A]"

      val smartCtorsClassImpl = mkSmartCtor(effectTrait, effectTyParams, requestType, requestDefs, requestClasses)
      val implicitInstance = mkCompanionDefaultInstance(effectTrait, smartCtorsClassImpl)
      val abstractInterpreter = mkAbstractInterpreter(effectTyParams, requestDefs, requestClasses)
      val injectInstance =
        q"implicit def injectInstance[F[_]](implicit I: cats.free.Inject[T, F]): cats.free.Inject[T, F] = I"

      q"""
        object $name {
          $requestTrait
          ..$requestClasses
          $requestTyAlias
          $smartCtorsClassImpl
          $implicitInstance
          def apply[..$effectTyParams](implicit c: ${effectTrait.name}[..${effectTyParams.map(_.name)}]):
              ${effectTrait.name}[..${effectTyParams.map(_.name)}] = c
          $abstractInterpreter
        }
      """
    }

    def getTypeParams(params: List[TypeDef]): List[TypeName] = {
      params.collect {
        case t: TypeDef if t.mods.hasFlag(Flag.PARAM) => t.name
      }
    }

    gen()
  }
}
