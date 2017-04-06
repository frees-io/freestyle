/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object freeImpl {

  object messages {

    val invalid = "Invalid use of the `@free` annotation"

    val abstractOnly = "The `@free` macro annotation can only be applied to either a trait or an abstract class."

    val noCompanion = "The trait or class annotated with `@free` must have no companion object."
  }

  def free(c: blackbox.Context)(annottees: c.Expr[Any]*): c.universe.Tree = {
    import c.universe._
    import c.universe.internal.reificationSupport._

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    def gen(): Tree = annottees match {
      case Expr(cls: ClassDef) :: Nil =>

        val effectTrait @ ClassDef(clsMods, _, _, _) = cls.duplicate
        if (clsMods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) {
          val effectObject = mkEffectObject(effectTrait)
          q"""
            $effectTrait
            $effectObject
          """
        } else fail(s"${messages.invalid} in ${cls.name}. ${messages.abstractOnly}")

      case Expr(cls: ClassDef) :: Expr(_) :: _ =>
        fail( s"${messages.invalid} in ${cls.name}. ${messages.noCompanion}")

      case _ => fail( s"${messages.invalid}. It is an annotation for traits or classes.")
    }

    // OP is the name of the Root trait of the Effect ADT
    lazy val OP = TypeName("Op")
    // MM Is the target of the Handler's natural Transformation
    lazy val MM = freshTypeName("MM$")
    // LL is the target of the Lifter's Injection
    lazy val LL = freshTypeName("LL$")
    // AA is the parameter inside type applications
    lazy val AA = freshTypeName("AA$")

    def isRequestDef(tree: Tree): Boolean = tree match {
      case q"$mods def $name[..$tparams](...$paramss): FreeS[..$args]" => true
      case q"$mods def $name[..$tparams](...$paramss): FreeS.Par[..$args]" => true
      case _ => false
    }

    class Request(reqDef: DefDef) {

      import reqDef.tparams

      val reqImpl = TermName(reqDef.name.toTermName.encodedName.toString)

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


      def mkRequestClass(effTTs: List[TypeDef]): ClassDef = {
        // Note: Effect trait type params are added to the request case class because its args may contain them
        val TTs = effTTs ++ tparams
        if (params.isEmpty)
          q"case class $Req[..$TTs]() extends $OP[$Res]"
        else
          q"case class $Req[..$TTs](..$params) extends $OP[$Res]"
      }

      def raiser: DefDef = {
        val injected = {
          /*filter: if !v.mods.hasFlag(Flag.IMPLICIT)*/
          val args = params.map(_.name)
          val ReqC = Req.toTermName
          q"FreeS.inject[$OP, $LL]( $ReqC[..${tparams.map(_.name)} ](..$args) )"
        }

        val tpt = reqDef.tpt.asInstanceOf[AppliedTypeTree]
        val (liftType, impl) = tpt match {
          case tq"FreeS    [$ff, $aa]" => ( tq"FreeS    [$LL, $aa]", q"FreeS.liftPar($injected)" )
          case tq"FreeS.Par[$ff, $aa]" => ( tq"FreeS.Par[$LL, $aa]",                  injected   )
          case _ => // Note: due to filter in getRequestDefs, this case is unreachable.
            fail(s"unknown abstract type found in @free container: $tpt : raw: ${showRaw(tpt)}")
        }

        q"override def ${reqDef.name}[..$tparams](...${reqDef.vparamss}): $liftType = $impl"
      }
    }

    def mkEffectObject(effectTrait: ClassDef) : ModuleDef= {

      val requests: List[Request] = effectTrait.impl.collect {
        case dd:DefDef if isRequestDef(dd) => new Request(dd.asInstanceOf[DefDef])
     }

      val Eff = effectTrait.name
      val TTs = effectTrait.tparams.tail
      val tns = TTs.map(_.name)
      val ev =  freshTermName("ev$")
      val ii =  freshTermName("ii$")
      val fa =  freshTermName("fa$")

      q"""
        object ${Eff.toTermName} {

          import cats.arrow.FunctionK
          import cats.free.Inject
          import freestyle.FreeS

          sealed trait $OP[$AA] extends scala.Product with java.io.Serializable
          ..${requests.map( _.mkRequestClass(TTs))}

          trait Handler[$MM[_], ..$TTs] extends FunctionK[$OP, $MM] {
            ..${requests.map( _.handlerDef )}

            override def apply[$AA]($fa: $OP[$AA]): $MM[$AA] = $fa match {
              case ..${requests.map(_.handlerCase )}
            }
          }

          class To[$LL[_], ..$TTs](implicit $ii: Inject[$OP, $LL]) extends $Eff[$LL, ..$tns] {
              ..${requests.map(_.raiser )}
          }

          implicit def to[$LL[_], ..$TTs](implicit $ii: Inject[$OP, $LL]):
              To[$LL, ..$tns] = new To[$LL, ..$TTs]

          def apply[$LL[_], ..$TTs](implicit $ev: $Eff[$LL, ..$tns]): $Eff[$LL, ..$tns] = $ev

        }
      """
    }

    gen()
  }
}
