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

import scala.reflect.macros.blackbox.Context

object taglessImpl {

  def tagless(c: Context)(annottees: c.Expr[Any]*): c.universe.Tree = {
    import c.universe._
    import c.universe.internal.reificationSupport._

    /* Macro Hygiene: we first declare a list of fresh names for term and types. */
    val MM = freshTypeName("MM$") // MM Target of the Handler's target M[_]

    def fail(msg: String) = c.abort(c.enclosingPosition, msg)

    // Messages of error
    val invalid = "Invalid use of the `@tagless` annotation"
    val abstractOnly = "The `@tagless` annotation can only be applied to a trait or to an abstract class."
    val noCompanion = "The trait (or class) annotated with `@tagless` must have no companion object."
    val onlyReqs = "In a `@tagless`-annotated trait (or class), all abstract method declarations should be of type FS[_]"

    def gen(): Tree = annottees match {
      case Expr(cls: ClassDef) :: Nil =>
        if (cls.mods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) {
          val effectTrait = cls.duplicate
          q"""
            ${mkEffectTrait(effectTrait.duplicate)}
            ${mkEffectObject(effectTrait.duplicate)}
          """
        } else fail(s"$invalid in ${cls.name}. $abstractOnly")

      case Expr(cls: ClassDef) :: Expr(_) :: _ => fail(s"$invalid in ${cls.name}. $noCompanion")

      case _ => fail(s"$invalid. $abstractOnly")
    }


    def mkEffectTrait(effectTrait: ClassDef): ClassDef = {
      val FF = freshTypeName("FF$")
      val requests: List[Request] = collectRequests(effectTrait)
      val body = requests.map(_.traitDef(FF))
      // this is to make a TypeDef for `$FF[_]`
      val wildcard = TypeDef(Modifiers(Flag.PARAM), typeNames.WILDCARD, List(), TypeBoundsTree(EmptyTree, EmptyTree))
      val ffTParam = TypeDef(Modifiers(Flag.PARAM), FF, List(wildcard), TypeBoundsTree(EmptyTree, EmptyTree))
      val ClassDef(mods, name, tparams, Template(parents, self, _)) = effectTrait
      ClassDef(mods, name, ffTParam :: tparams, Template(parents, self, body))
    }

    class Request(reqDef: DefDef) {

      import reqDef.tparams

      val reqImpl = TermName(reqDef.name.toTermName.encodedName.toString)

      private[this] val Res = reqDef.tpt.asInstanceOf[AppliedTypeTree].args.last

      val params: List[ValDef] = reqDef.vparamss.flatten

      def freeDef: DefDef =
        if (params.isEmpty)
          q"def $reqImpl[..$tparams]: FS[$Res]"
        else
          q"def $reqImpl[..$tparams](..$params): FS[$Res]"

      def freeHandlerDef(H: TermName, RT: TypeName): DefDef = {
        val args = params.map(_.name)
        if (params.isEmpty)
          q"def $reqImpl[..$tparams]: $RT[$Res] = $H.${reqDef.name}(..$args)"
        else
          q"def $reqImpl[..$tparams](..$params): $RT[$Res] = $H.${reqDef.name}(..$args)"
      }

      def stackSafeFinallyTaglessHandlerDef(H: TermName, RT: TypeName): DefDef = {
        val args = params.map(_.name)
        if (params.isEmpty)
          q"""
            def $reqImpl[..$tparams]: _root_.cats.free.Free[$RT, $Res] =
              _root_.cats.free.Free.liftF($H.${reqDef.name}(..$args))
          """
        else
          q"""
            def $reqImpl[..$tparams](..$params): _root_.cats.free.Free[$RT, $Res] =
              _root_.cats.free.Free.liftF($H.${reqDef.name}(..$args))
          """
      }

      def traitDef(FF: TypeName): DefDef =
        if (params.isEmpty)
          q"def $reqImpl[..$tparams]: $FF[$Res]"
        else
          q"def $reqImpl[..$tparams](..$params): $FF[$Res]"

      def handlerDef: DefDef =
        if (params.isEmpty)
          q"def $reqImpl[..$tparams]: $MM[$Res]"
        else
          q"def $reqImpl[..$tparams](..$params): $MM[$Res]"

    }

    def collectRequests(effectTrait: ClassDef): List[Request] = effectTrait.impl.collect {
      case dd@q"$mods def $name[..$tparams](...$paramss): $tyRes" => tyRes match {
        case tq"FS[..$args]" => new Request(dd.asInstanceOf[DefDef])
        case _ => fail(s"$invalid in definition of method $name in ${effectTrait.name}. $onlyReqs")
      }
    }

    def mkEffectObject(effectTrait: ClassDef): ModuleDef = {

      val requests: List[Request] = collectRequests(effectTrait)

      val Eff = effectTrait.name
      val TTs = effectTrait.tparams
      val tns = TTs.map(_.name)
      val ev = freshTermName("ev$")
      val hh = freshTermName("hh$")
      val stackSafeHandler = freshTermName("stackSafeHandler$")
      val stackSafeFTHandler = freshTermName("stackSafeFTHandler$")

      q"""
        object ${Eff.toTermName} {

          trait Handler[$MM[_], ..$TTs] extends $Eff[$MM, ..$tns] {
            ..${requests.map(_.handlerDef)}
          }

          @_root_.freestyle.free trait StackSafe {
            ..${requests.map(_.freeDef)}
          }

          implicit def $stackSafeHandler[$MM[_]: _root_.cats.Monad](implicit $hh: Handler[$MM]): StackSafe.Handler[$MM] = new StackSafe.Handler[$MM] {
            ..${requests.map(_.freeHandlerDef(hh, MM))}
          }

          implicit def $stackSafeFTHandler[$MM[_]: _root_.cats.Monad](
            implicit $hh: Handler[$MM]
          ): Handler[({ type λ[α] = _root_.cats.free.Free[$MM, α]})#λ] =
            new Handler[({ type λ[α] = _root_.cats.free.Free[$MM, α]})#λ] {
              ..${requests.map(_.stackSafeFinallyTaglessHandlerDef(hh, MM))}
            }

          def apply[$MM[_], ..$TTs](implicit $ev: $Eff[$MM, ..$tns]): $Eff[$MM, ..$tns] = $ev

        }
      """
    }

    val output = gen()
    println(output)
    output
  }
}
