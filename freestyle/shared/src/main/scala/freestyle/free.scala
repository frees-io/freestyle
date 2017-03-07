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

    // OP is the name of the Root trait of the Effect ADT
    lazy val OP = TypeName("T")
    // MM Is the target of the Interpreter's natural Transformation
    lazy val MM = TypeName("MM")
    // LL is the target of the Lifter's Injection
    lazy val LL = TypeName("LL")
    // LIFT is the name of the Lifter's Injector Class: no sense in separate name
    lazy val LIFT = TypeName("Lift")

    class Request(reqDef: DefDef) {

      val delegate = TermName(reqDef.name.toTermName.encodedName.toString + "Impl")

      val returnType = reqDef.tpt.asInstanceOf[AppliedTypeTree].args.last
      private[this] val Res = returnType

      val params: List[ValDef] = reqDef.vparamss.flatten

      val className: TypeName = TypeName(reqDef.name.toTypeName.encodedName.toString.capitalize + "OP")

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

      def handlerDef = {
        val TTs = reqDef.tparams
        // args has at least one element
        if (params.isEmpty)
          q"protected[this] def $delegate[..$TTs]: $MM[$Res]"
        else
          q"protected[this] def $delegate[..$TTs](..$params): $MM[$Res]"
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
      def mkRequestClass(effectTyParams: List[TypeDef]): ClassDef = {
        // Note: Effect trait type params are added to the request case class because its args may contain them
        val TTs = effectTyParams.tail ++ reqDef.tparams
        val ReqC = className
        if (params.isEmpty)
            q"""case class $ReqC[..$TTs]() extends $OP[$Res] """
        else
            q"""case class $ReqC[..$TTs](..$params) extends $OP[$Res]"""
      }

      def lifter(cpType: TypeName): DefDef = {
        val injected = {
          val tparams = reqDef.tparams
          /*filter: if !v.mods.hasFlag(Flag.IMPLICIT)*/
          val args = params.map(_.name)
          val ReqC = className.toTermName

          q"FreeS.inject[$OP, $cpType]( $ReqC[..${tparams.map(_.name)} ](..$args) )"
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

    def mkLifterClass(
      effectName: TypeName,
      effectTyParams: List[TypeDef],
      requests: List[Request]
    ): ClassDef = {
      // Constraint: cpTypes non empty
      val cpType: TypeName = effectTyParams.filter(_.mods.hasFlag(Flag.PARAM)).head.name

      val reqLifters = requests.map(_.lifter(cpType) )
      val FF = effectTyParams.head.name
      val TTs = effectTyParams.tail

      q"""
       private[this] class $LIFT[$FF[_], ..$TTs](implicit I: Inject[$OP, $FF])
          extends $effectName[$FF, ..${TTs.map(_.name)}] {
            ..$reqLifters
          }
      """
    }

    def mkLifterFactory( effectName: TypeName, effectTyParams: List[TypeDef]): DefDef = {
      val FF = effectTyParams.head.name
      val tts = effectTyParams.tail
      q"""
        implicit def defaultInstance[$FF[_], ..$tts](
            implicit I: Inject[$OP, $FF]
        ): $effectName[$FF, ..${tts.map(_.name)}] = 
            new $LIFT[$FF, ..$tts]
      """
    }

    def mkEffectHandler( effectTyParams: List[TypeDef], requests: List[Request]): ClassDef = {
      val delegates = requests.map( _.handlerDef )
      val cases = requests.map(_.handlerCase )
      val TTs = effectTyParams.tail

      q"""trait Interpreter[$MM[_], ..$TTs] extends FunctionK[$OP, $MM] {
            ..$delegates
            override def apply[A](fa: $OP[A]): $MM[A] = fa match { case ..$cases }
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
      val requestClasses : List[ClassDef] = requests.map( _.mkRequestClass(effectTyParams))
      val lifterClass = mkLifterClass(effectName, effectTyParams, requests)
      val lifterFactory = mkLifterFactory(effectName, effectTyParams)
      val effectHandler = mkEffectHandler(effectTyParams, requests)

      q"""
        object ${effectName.toTermName} {
          import cats.arrow.FunctionK
          import cats.free.Inject
          import freestyle.FreeS
          sealed trait $OP[A] extends Product with Serializable
          ..$requestClasses
          $lifterClass
          $lifterFactory
          def apply[..$effectTyParams](implicit c: $effectName[..${effectTyParams.map(_.name)}]):
              $effectName[..${effectTyParams.map(_.name)}] = c
          $effectHandler
        }
      """
    }

    gen()
  }
}
