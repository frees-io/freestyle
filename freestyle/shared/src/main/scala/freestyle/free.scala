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

    def classNameFor(reqDef: DefDef): TypeName =
      toRequestAdtName(reqDef.name.toTypeName)

    def mkRequestTrait(requestTypeName: TypeName) =
      q"sealed trait $requestTypeName[A] extends Product with Serializable"


      /* A Request declaration in an Effect Trait, such as
       *
       * @free trait UserRepository[F[_]] {
       *     def get(id: Long): FreeS[F, User]
       *
       * gets translated to a Request class such as
       *
       *     case class Get(id: Long) extends UserRepositoryOp[User]
       */
      def mkRequestClass(effectTyParams: List[TypeDef], requestType: TypeName, reqDef: DefDef): ClassDef = {
        val retType = reqDef.tpt.asInstanceOf[AppliedTypeTree].args.last
        // Note: Effect trait type params are added to the request case class because its args may contain them
        val tparams = effectTyParams.tail ++ reqDef.tparams
        val args    = reqDef.vparamss.flatten //.filter(v => !v.mods.hasFlag(Flag.IMPLICIT))
        val className = classNameFor(reqDef)
        args match {
          case Nil =>
            q"""case class $className[..$tparams]() extends $requestType[$retType] """
          case _ =>
            q"""case class $className[..$tparams](..$args) extends $requestType[$retType]"""
        }
      }

    def mkSmartCtor(
      effectName: TypeName,
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
        requestClass: ClassDef): DefDef = {

        val ctor = requestClass.find {
          case DefDef(_, TermName("<init>"), _, _, _, _) => true
          case _                                         => false
        }.get

        val args = requestDef.vparamss.flatten.collect {
          case v /*if !v.mods.hasFlag(Flag.IMPLICIT)*/ => v.name
        }
        val reqClassName = requestClass.name

        val companionApply = q"${reqClassName.toTermName}[..${requestClass.tparams.map(_.name)}](..$args)"

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
        effectName: TypeName,
        effectTyParams: List[TypeDef],
        smartCtorsImpl: ClassDef): DefDef = {
      q"""
        implicit def defaultInstance[..$effectTyParams](
            implicit I: cats.free.Inject[T, ${effectTyParams.head.name}]
        ): $effectName[..${effectTyParams.map(_.name)}] =
            new ${smartCtorsImpl.name}[..${effectTyParams.map(_.name)}]
      """
    }

    def mkEffectHandler(
        effectTyParams: List[TypeDef],
        requestDefs: List[DefDef]): ClassDef = {

      val firstTParam = effectTyParams.head

      def mkMatchCase(reqDef: DefDef): CaseDef  = {
        val wildcardsArgsTpl = reqDef.vparamss.flatten.collect {
          case v /*if !v.mods.hasFlag(Flag.IMPLICIT)*/ => (q"l.${v.name}", pq"_")
        }
        val fields = wildcardsArgsTpl.map(_._1)
        val wildcards = wildcardsArgsTpl.map(_._2)
        val delegate = TermName(reqDef.name.toTermName.encodedName.toString + "Impl")
        val className = classNameFor(reqDef)
        val pattern = pq"l @ ${className.toTermName}(..$wildcards)"
        wildcardsArgsTpl match {
          case Nil => cq"$pattern => $delegate"
          case _   => cq"$pattern => $delegate(..$fields)"
        }
      }

      def mkRequestHandler(reqDef: DefDef) = {
        val implName = TermName(reqDef.name.toTermName.encodedName.toString + "Impl")
        // args has at least one element
        val retType = reqDef.tpt.asInstanceOf[AppliedTypeTree].args.last
        val params = reqDef.vparamss.flatten
        params match {
          case Nil => q"protected[this] def $implName[..${reqDef.tparams}]: ${firstTParam.name}[$retType]"
          case _   => q"protected[this] def $implName[..${reqDef.tparams}](..$params): ${firstTParam.name}[$retType]"
        }
      }

      val abstractImpls = requestDefs.map( reqDef => mkRequestHandler(reqDef) )
      val matchCases = requestDefs.map( reqDef => mkMatchCase(reqDef) )

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

      val effectName: TypeName = effectTrait.name
      val effectTyParams: List[TypeDef] = effectTrait.tparams

      val requestDefs    : List[DefDef]   = getRequestDefs(effectTrait)
      val requestType    : TypeName       = toRequestAdtName(effectName)
      val requestTrait   : ClassDef       = mkRequestTrait(requestType)
      val requestClasses : List[ClassDef] = requestDefs.map( sc => mkRequestClass(effectTyParams, requestType, sc))
      val requestTyAlias = q"type T[A] = $requestType[A]"

      val smartCtorsClassImpl = mkSmartCtor(effectName, effectTyParams, requestType, requestDefs, requestClasses)
      val implicitInstance = mkCompanionDefaultInstance(effectName, effectTyParams, smartCtorsClassImpl)
      val effectHandler = mkEffectHandler(effectTyParams, requestDefs)

      q"""
        object ${effectName.toTermName} {
          $requestTrait
          ..$requestClasses
          $requestTyAlias
          $smartCtorsClassImpl
          $implicitInstance
          def apply[..$effectTyParams](implicit c: $effectName[..${effectTyParams.map(_.name)}]):
              $effectName[..${effectTyParams.map(_.name)}] = c
          $effectHandler
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
