package freestyle

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

@compileTimeOnly("enable macro paradise to expand @free macro annotations")
class free extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro free.impl
}

object free {

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.universe.Tree = {
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

    def toRequestAdtName(name: TypeName) =
      TypeName(name.encodedName.toString.capitalize + "OP")

    def mkRequestTrait(requestType: TypeName): ClassDef =
      q"sealed trait $requestType[A] extends Product with Serializable"

    class Request(reqDef: DefDef) {

      val delegate = TermName(reqDef.name.toTermName.encodedName.toString + "Impl")

      val returnType = reqDef.tpt.asInstanceOf[AppliedTypeTree].args.last
      private[this] val Res = returnType

      val params: List[ValDef] = reqDef.vparamss.flatten

      val className: TypeName = toRequestAdtName(reqDef.name.toTypeName)

      def handlerCase: CaseDef  = {
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

      def handlerDef(FF: TypeName) = {
        val TTs = reqDef.tparams
        // args has at least one element
        if (params.isEmpty)
          q"protected[this] def $delegate[..$TTs]: $FF[$Res]"
        else
          q"protected[this] def $delegate[..$TTs](..$params): $FF[$Res]"
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
      def mkRequestClass(effectTyParams: List[TypeDef], ReqT: TypeName): ClassDef = {
        // Note: Effect trait type params are added to the request case class because its args may contain them
        val TTs = effectTyParams.tail ++ reqDef.tparams
        val ReqC = className
        if (params.isEmpty)
            q"""case class $ReqC[..$TTs]() extends $ReqT[$Res] """
        else
            q"""case class $ReqC[..$TTs](..$params) extends $ReqT[$Res]"""
      }

      def lifter(requestType: TypeName, cpType: TypeName, effectTyParams: List[TypeDef]): DefDef = {
        /*filter: if !v.mods.hasFlag(Flag.IMPLICIT)*/
        val tparams = effectTyParams.tail ++ reqDef.tparams
        val injected = {
          val args = params.map(_.name)
          val ReqC = className.toTermName
          val ReqT = requestType

          q"FreeS.inject[$ReqT, $cpType]( $ReqC[..${tparams.map(_.name)} ](..$args) )"
        }

        val tpt = reqDef.tpt.asInstanceOf[AppliedTypeTree].tpt
        val impl = tpt match {
          case Ident(TypeName(tp)) if tp.endsWith("FreeS") =>
            q"FreeS.liftPar($injected)"
          case Select(Ident(TermName(term)), TypeName(tp)) if tp.endsWith("Par") =>
            injected
          case _ => // Note: due to filter in getRequestDefs, this case is unreachable.
            fail(s"unknown abstract type found in @free container: $tpt : raw: ${showRaw(tpt)}")
        }

        q"override def ${reqDef.name}[..${reqDef.tparams}](...${reqDef.vparamss}): ${reqDef.tpt} = $impl"
      }
    }

    def mkLifter(
      effectName: TypeName,
      effectTyParams: List[TypeDef],
      requestType: TypeName,
      requests: List[Request]
    ): ClassDef = {
      // Constraint: cpTypes non empty
      val cpType: TypeName = getTypeParams(effectTyParams).head
      val reqCtors = requests.map(_.lifter(requestType, cpType, effectTyParams) )

      val liftC  = TypeName( effectName.decodedName.toString + "_default_impl")
      val FF = effectTyParams.head.name

      q"""
       private[this] class $liftC[..$effectTyParams](implicit I: Inject[T, $FF])
          extends $effectName[..${effectTyParams.map(_.name)}] {
            ..$reqCtors
          }
      """
    }


    def mkDefaultInstance(
        effectName: TypeName,
        effectTyParams: List[TypeDef],
        lifter: ClassDef): DefDef = {
      val FF = effectTyParams.head.name
      val tts = effectTyParams.map(_.name)
      q"""
        implicit def defaultInstance[..$effectTyParams]( implicit I: Inject[T, $FF] ): $effectName[..$tts] =
            new ${lifter.name}[..$tts]
      """
    }

    def mkEffectHandler( effectTyParams: List[TypeDef], requests: List[Request]): ClassDef = {
      val FF = effectTyParams.head.name
      val delegates = requests.map( _.handlerDef(FF) )
      val cases = requests.map(_.handlerCase )

      q"""trait Interpreter[..$effectTyParams] extends FunctionK[T, $FF] {
            ..$delegates
            override def apply[A](fa: T[A]): $FF[A] = fa match { case ..$cases }
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

      val requests       : List[Request]  = getRequestDefs(effectTrait).map( p => new Request(p))
      val requestType    : TypeName       = toRequestAdtName(effectName)
      val requestTrait   : ClassDef       = mkRequestTrait(requestType)
      val requestClasses : List[ClassDef] = requests.map( _.mkRequestClass(effectTyParams, requestType))
      val requestTyAlias = q"type T[A] = $requestType[A]"

      val lifter = mkLifter(effectName, effectTyParams, requestType, requests)
      val implicitInstance = mkDefaultInstance(effectName, effectTyParams, lifter)
      val effectHandler = mkEffectHandler(effectTyParams, requests)

      q"""
        object ${effectName.toTermName} {
          import cats.arrow.FunctionK
          import cats.free.Inject
          import freestyle.FreeS

          $requestTrait
          ..$requestClasses
          $requestTyAlias
          $lifter
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
