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

package freestyle.macroimpl

import scala.annotation.tailrec
import scala.reflect.macros.whitebox.Context
import scala.util.Try

import cats.data.NonEmptyList
import cats.data.{ Validated, ValidatedNel }
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import cats.syntax.validated._
import cats.instances.all._

trait FreeModuleLike

// $COVERAGE-OFF$ ScalaJS + coverage = fails with NoClassDef exceptions

final class moduleImpl(val c: Context) {
  import c.universe.{ Try => _, _ }
  import c.universe.internal.reificationSupport.{ ConstantType => _, _ }
  import compat._

  private[this] lazy val FF       = freshTypeName("FF")
  private[this] lazy val LL       = freshTypeName("LL")
  private[this] lazy val AA       = freshTypeName("AA")

  private[this] lazy val tpnme_EMPTY_PACKAGE_NAME = termNames.EMPTY_PACKAGE_NAME.toTypeName

  private[this] lazy val FreeModuleLike = typeOf[freestyle.macroimpl.FreeModuleLike].typeSymbol
  private[this] lazy val Coproduct      = typeOf[cats.data.Coproduct[Nothing, Nothing, _]].typeSymbol
  private[this] lazy val CopK           = typeOf[iota.CopK[_, _]].typeSymbol
  private[this] lazy val KNil           = typeOf[iota.KNil].typeSymbol
  private[this] lazy val KCons          = typeOf[iota.KCons[Nothing, _]].typeSymbol

  private[this] lazy val wildcard =
    TypeDef(Modifiers(Flag.PARAM), typeNames.WILDCARD, List(),
      TypeBoundsTree(EmptyTree, EmptyTree))

  private[this] lazy val FFtypeConstructor =
    TypeDef(Modifiers(Flag.PARAM), FF, List(wildcard),
      TypeBoundsTree(EmptyTree, EmptyTree))

  private[this] implicit final class ValidateFlatmap[E, A](v: Validated[E, A]) {
    def flatMap[EE >: E, B](f: A => Validated[EE, B]): Validated[EE, B] =
      v.andThen(f)
  }

  def impl(annottees: c.Expr[Any]*): Tree =
    foldAbort(for {
      cls             <- decodeAnnotationTarget(annottees.toList)
      effectTuples     = gatherEffects(cls)
      classTree       <- makeClassTree(cls)
      coproductTree   <- makeCoproductTree(effectTuples)
      objectTree      <- makeModuleTree(cls, coproductTree, effectTuples)
    } yield q"$classTree; $objectTree")

  private[this] def decodeAnnotationTarget(annottees: List[c.Expr[Any]]) =
    annottees.map(_.tree) match {
      case (cls: ClassDef) :: Nil =>
        if (cls.mods.hasFlag(Flag.TRAIT | Flag.ABSTRACT)) cls.validNel
        else
          "The `@module` annotation can only be applied to a trait or an abstract class.".invalidNel

      case (cls: ClassDef) :: more =>
        "The trait or class annotated with `@module` must have no companion object.".invalidNel
      case trees =>
        "Unexpected trees $trees encountered for `@module` annotation".invalidNel
  }

  private[this] def gatherEffects(cls: ClassDef): List[(Name, Tree)] =
    cls.impl.body.collect {
      case valDef: ValDef if valDef.rhs.isEmpty => (valDef.name, valDef.tpt) }

  private[this] def makeClassTree(cls: ClassDef): ValidatedNel[String, ClassDef] = {
    val ClassDef(mods, name, tparams, Template(parents, self, body)) = cls
    val body0 = body.map {
      case q"$mods val $name: $tpt[..$args]" => q"$mods val $name: $tpt[$FF, ..$args]"
      case x => x
    }
    val parentTrees = parents ++ List(tq"_root_.freestyle.macroimpl.FreeModuleLike", tq"_root_.freestyle.macroimpl.EffectLike[$FF]")
    ClassDef(mods, name, FFtypeConstructor :: tparams,
      Template(parentTrees, self, body0)).validNel
  }

  private[this] def makeCoproductTree(effectTuples: List[(Name, Tree)]): ValidatedNel[String, Tree] = {

    val Ltpes = effectTuples.map(_._2).traverse { t =>
      val treeCode = s"type Z = $t.OpTypes"
      // (‡▼益▼)<!! would love to find a better solution (instead of c.parse)
      try {
        val q"type Z = $select" = c.parse(treeCode)
        select.validNel
      } catch {
        case t: Throwable => s"Error ${t.getMessage} when parsing $treeCode".invalidNel
      }
    }

    Ltpes
      .map {
        case Nil         => tq"_root_.iota.KNil"
        case tree :: Nil => tree
        case trees       => trees.reduce((l, r) => tq"_root_.iota.KList.Op.Concat[$l, $r]")
      }
      .map(L =>
        q"""
          type OpTypes = $L
          type Op[A]  = _root_.iota.CopK[OpTypes, A]""")
  }

  private[this] def makeModuleTree(
    cls: ClassDef,
    coproductTree: Tree,
    effects: List[(Name, Tree)]
  ): ValidatedNel[String, ModuleDef] = {

    import cls.{ tparams, name }

    val builtArgs = effects.traverse {
      case (n, tq"$tpt[..$args]") => q"""${n.toTermName}: $tpt[$LL, ..$args]""".validNel
      case (n, t) => s"unexpected type $n: $t when building args".invalidNel
    }

    builtArgs.map(args => q"""
      object ${name.toTermName} {
        ..$coproductTree
        def apply[$LL[_], ..$tparams](implicit ev: $name[$LL, ..$tparams]): $name[$LL, ..$tparams] = ev
        implicit def to[$LL[_], ..$tparams](implicit ..$args): To[$LL, ..$tparams] =
          new To[$LL, ..$tparams]()
        class To[$LL[_], ..$tparams](implicit ..$args) extends $name[$LL, ..$tparams]
      }""")
  }

  private[this] def foldAbort[A](v: ValidatedNel[String, A]): A =
    v fold (
      errors => c.abort(c.enclosingPosition, errors.toList.mkString(", and\n")),
      a      => a)
}
