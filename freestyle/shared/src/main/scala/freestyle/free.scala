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


    class Request(reqDef: DefDef) {

      val delegate = TermName(reqDef.name.toTermName.encodedName.toString + "Impl")

      val returnType = reqDef.tpt.asInstanceOf[AppliedTypeTree].args.last
      private[this] val Res = returnType

      val params = reqDef.vparamss.flatten

      val className: TypeName = toRequestAdtName(reqDef.name.toTypeName)

      def mkMatchCase(): CaseDef  = {
        // filter: !v.mods.hasFlag(Flag.IMPLICIT)
        val fields = params.map( v => q"l.${v.name}")
        val wildcards = params.map( v => pq"_")
        val ReqC = className.toTermName
        val pattern = pq"l @ $ReqC(..$wildcards)"

        if (fields.isEmpty)
          cq"$pattern => $delegate"
        else
          cq"$pattern => $delegate(..$fields)"
      }

      def mkRequestHandler(firstTParam: TypeDef) = {
        val FF = firstTParam.name
        val tts = reqDef.tparams
        // args has at least one element
        if (params.isEmpty)
          q"protected[this] def $delegate[..$tts]: $FF[$Res]"
        else
          q"protected[this] def $delegate[..$tts](..$params): $FF[$Res]"
      }


      /* A Request declaration in an Effect Trait, such as
       *
       * @free trait UserRepository[F[_]] {
       *     def get(id: Long): FreeS[F, User]
       *
       * gets translated to a Request class such as
       *
       *     case class Get(id: Long) extends UserRepositoryOp[User]
       */
      def mkRequestClass(effectTyParams: List[TypeDef], requestType: TypeName): ClassDef = {
        // Note: Effect trait type params are added to the request case class because its args may contain them
        val tts = effectTyParams.tail ++ reqDef.tparams
        val ReqT = requestType
        val ReqC = className
        if (params.isEmpty)
            q"""case class $ReqC[..$tts]() extends $ReqT[$Res] """
        else
            q"""case class $ReqC[..$tts](..$params) extends $ReqT[$Res]"""
      }

      def mkSmartCtorImpl(requestType: TypeName, cpType: TypeName, effectTyParams: List[TypeDef]): DefDef = {
        /*filter: if !v.mods.hasFlag(Flag.IMPLICIT)*/
        val args = params.map(_.name)
        val tparams = effectTyParams.tail ++ reqDef.tparams
        val companionApply = q"${className.toTermName}[..${tparams.map(_.name)}](..$args)"

        val inject = q"freestyle.FreeS.inject[$requestType, $cpType]($companionApply)"
        val tpt = reqDef.tpt.asInstanceOf[AppliedTypeTree].tpt
        val impl = tpt match {
          case Ident(TypeName(tp)) if tp.endsWith("FreeS") =>
            q"freestyle.FreeS.liftPar($inject)"
          case Select(Ident(TermName(term)), TypeName(tp)) if tp.endsWith("Par") =>
            inject
          case _ =>
            fail(s"unknown abstract type found in @free container: $tpt : raw: ${showRaw(tpt)}")
        }
        q"override def ${reqDef.name}[..${reqDef.tparams}](...${reqDef.vparamss}): ${reqDef.tpt} = $impl"

      }


    }

    def mkSmartCtor(
      effectName: TypeName,
      effectTyParams: List[TypeDef],
      requestType: TypeName,
      requestDefs: List[DefDef]
    ): ClassDef = {
      // Constraint: cpTypes non empty
      val cpType: TypeName = getTypeParams(effectTyParams).head
      val smartCtorsImpls =
        requestDefs.map(p => new Request(p).mkSmartCtorImpl(requestType, cpType, effectTyParams) )

      val implName    = TypeName( effectName.decodedName.toString + "_default_impl")
      val FF = effectTyParams.head.name

      q"""
       class $implName[..$effectTyParams](implicit I: cats.free.Inject[T, $FF])
          extends $effectName[..${effectTyParams.map(_.name)}] {
            ..$smartCtorsImpls
          }
      """
    }


    def mkCompanionDefaultInstance(
        effectName: TypeName,
        effectTyParams: List[TypeDef],
        smartCtorsImpl: ClassDef): DefDef = {
      val FF = effectTyParams.head.name
      val tts = effectTyParams.map(_.name)
      q"""
        implicit def defaultInstance[..$effectTyParams](
            implicit I: cats.free.Inject[T, $FF]
        ): $effectName[..$tts] =
            new ${smartCtorsImpl.name}[..$tts]
      """
    }

    def mkEffectHandler(
        effectTyParams: List[TypeDef],
        requestDefs: List[DefDef]): ClassDef = {

      val FF = effectTyParams.head.name
      val abstractImpls = requestDefs.map( reqDef => new Request(reqDef).mkRequestHandler(effectTyParams.head) )
      val matchCases = requestDefs.map( reqDef => new Request(reqDef).mkMatchCase() )

      q"""abstract class Interpreter[..$effectTyParams] extends cats.arrow.FunctionK[T, $FF] {
            ..$abstractImpls
            override def apply[A](fa: T[A]): $FF[A] = fa match {case ..$matchCases}
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
      val requestClasses : List[ClassDef] =
        requestDefs.map( reqDef => new Request(reqDef).mkRequestClass(effectTyParams, requestType))
      val requestTyAlias = q"type T[A] = $requestType[A]"

      val smartCtorsClassImpl = mkSmartCtor(effectName, effectTyParams, requestType, requestDefs)
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
