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

      import reqDef.tparams

      val reqImpl = TermName(reqDef.name.toTermName.encodedName.toString + "Impl")

      // Name of the Request ADT Class
      private[this] val Req: TypeName = TypeName(reqDef.name.toTypeName.encodedName.toString.capitalize + "OP")
      private[this] val Res = reqDef.tpt.asInstanceOf[AppliedTypeTree].args.last

      val params: List[ValDef] = reqDef.vparamss.flatten

      def handlerCase: CaseDef  = {
        val ReqC = Req.toTermName
        if (params.isEmpty)
          cq"$ReqC() => $reqImpl"
        else {
          // filter: !v.mods.hasFlag(Flag.IMPLICIT)
          val ffs = params.map( v => q"l.${v.name}")
          val uss = params.map( v => pq"_")
          cq"l @ $ReqC(..$uss) => $reqImpl(..$ffs)"
        }

      }

      def handlerDef: DefDef =
        if (params.isEmpty)
          q"protected[this] def $reqImpl[..$tparams]: $MM[$Res]"
        else
          q"protected[this] def $reqImpl[..$tparams](..$params): $MM[$Res]"


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
        val TTs = effectTyParams.tail ++ tparams
        if (params.isEmpty)
          q"case class $Req[..$TTs]() extends $OP[$Res]"
        else
          q"case class $Req[..$TTs](..$params) extends $OP[$Res]"
      }

      def lifter(FF: TypeName): DefDef = {
        val injected = {
          /*filter: if !v.mods.hasFlag(Flag.IMPLICIT)*/
          val args = params.map(_.name)
          val ReqC = Req.toTermName

          q"FreeS.inject[$OP, $FF]( $ReqC[..${tparams.map(_.name)} ](..$args) )"
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

        q"override def ${reqDef.name}[..$tparams](...${reqDef.vparamss}): ${reqDef.tpt} = $impl"
      }
    }

    def mkLifterClass( effectName: TypeName, effectTyParams: List[TypeDef], requests: List[Request] ): ClassDef = {
      // Constraint: cpTypes non empty
      val FF = effectTyParams.head.name
      val TTs = effectTyParams.tail

      q"""
       class $LIFT[$FF[_], ..$TTs](implicit I: Inject[$OP, $FF])
          extends $effectName[$FF, ..${TTs.map(_.name)}] {
            ..${requests.map(_.lifter(FF) )}
          }
      """
    }

    def mkLifterFactory( eff: TypeName, effTTs: List[TypeDef]): DefDef =
      q"""
        implicit def defaultInstance[$LL[_], ..$effTTs](implicit I: Inject[$OP, $LL]): 
            $eff[$LL, ..${effTTs.map(_.name)}] = new $LIFT[$LL, ..$effTTs]
      """

    def mkHandler( effTTs: List[TypeDef], requests: List[Request]): ClassDef =
      q"""
        trait Interpreter[$MM[_], ..$effTTs] extends FunctionK[$OP, $MM] {
          ..${requests.map( _.handlerDef )}
          override def apply[A](fa: $OP[A]): $MM[A] = fa match { 
            case ..${requests.map(_.handlerCase )}
          }
        }
      """

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
      val lifterFactory = mkLifterFactory(effectName, effectTyParams.tail)
      val effectHandler = mkHandler(effectTyParams.tail, requests)

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
