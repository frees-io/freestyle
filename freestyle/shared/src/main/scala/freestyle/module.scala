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

import scala.annotation.tailrec
import scala.reflect.macros.whitebox.Context

import cats.data.NonEmptyList
import cats.data.{ Validated, ValidatedNel }
import cats.syntax.flatMap._
import cats.syntax.traverse._
import cats.syntax.validated._
import cats.instances.all._

trait FreeModuleLike

// $COVERAGE-OFF$ ScalaJS + coverage = fails with NoClassDef exceptions

final class moduleImpl(val c: Context) {
  import c.universe._
  import c.universe.internal.reificationSupport.{ ConstantType => _, _ }
  import compat._

  private[this] lazy val FF       = freshTypeName("FF")
  private[this] lazy val LL       = freshTypeName("LL")
  private[this] lazy val AA       = freshTypeName("AA")

  private[this] lazy val tpnme_EMPTY_PACKAGE_NAME = termNames.EMPTY_PACKAGE_NAME.toTypeName

  private[this] lazy val FreeModuleLike = typeOf[freestyle.FreeModuleLike].typeSymbol
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
      objectTree      <- makeModuleTree(cls, effectTuples)
    } yield q"$classTree; $objectTree")

  def computeCoproductImpl: Tree =
    foldAbort(for {
      tpe             <- compassionateCompanionTypeOf(c.internal.enclosingOwner.owner)
      fullEffectTypes =  expandEffectType(tpe)
      fullEffectTrees <- fullEffectTypes.traverse(typeToTypeTree)
      //coproductTree  = makeCatsCoproduct(fullEffectTrees) // TODO: make this configurable?
      coproductTree    = makeIotaCoproduct(fullEffectTrees)
    } yield q"""new { ..$coproductTree }""")

  def compassionateCompanionTypeOf(sym: Symbol): ValidatedNel[String, Type] =
    if (sym.companion.isType)
      sym.companion.asType.toType.validNel
    else
      s"unable to find companion for $sym when expanding coproduct".invalidNel

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

  private[this] def expandEffectType(tpe: Type): List[Type] = {
    def isModuleFS(cs: ClassSymbol): Boolean =
      cs.baseClasses.exists(_.name == TypeName("FreeModuleLike"))

    def expandClass(cs: ClassSymbol): List[Type] =
      cs.info.decls.toList
        .collect { case met: MethodSymbol if met.isAbstract => met }
        .flatMap(x => expandEffectType(x.returnType))

    tpe.typeSymbol match {
      case cs: ClassSymbol =>
        if (isModuleFS(cs)) expandClass(cs) else List(tpe.typeConstructor)
      case _ => Nil
    }
  }

  private[this] def typeToTypeTree(tpe: Type): ValidatedNel[String, Tree] =
    Validated.catchNonFatal(c.parse(tpe.toString)).leftMap(t =>
      NonEmptyList.of(s"unable to convert type $tpe to tree"))

  private[this] lazy val defaultf0: Tree =
    q"type Op[$AA] = Nothing"
  private[this] def defaultf1(tpt: Tree): Tree =
    q"type Op[$AA] = $tpt.Op[$AA]"

  private[this] def makeCatsCoproduct(
    tpts: List[Tree]
  ): Tree = tpts match {
    case Nil                    => defaultf0
    case tpt :: Nil             => defaultf1(tpt)
    case head1 :: head2 :: tail =>

      val max = tail.length
      def Op(i: Int) = (max - i - 1) match {
        case 0 => TypeName("Op")
        case n => TypeName(s"Op$n")
      }

      tail.zipWithIndex.foldLeft(
        q"type ${Op(-1)}[$AA] = $Coproduct[$head2.Op, $head1.Op, $AA]": Tree
      ) { (prefix, tup) =>
        val (tpt, i) = tup
        q"""
          ..$prefix
          type ${Op(i)}[$AA] = $Coproduct[$tpt.Op, ${Op(i - 1)}, $AA]
        """
      }
  }

  private[this] def makeIotaCoproduct(
    tpts: List[Tree]
  ): Tree = tpts match {
    case Nil        => defaultf0
    case tpt :: Nil => defaultf1(tpt)
    case _          =>
      val klist = tpts.foldLeft(tq"$KNil": Tree)((tail, head) =>
        tq"$KCons[$head.Op, $tail]")
      q"type Op[$AA] = $CopK[$klist, $AA]"
  }

  private[this] def makeClassTree(cls: ClassDef): ValidatedNel[String, ClassDef] = {
    val ClassDef(mods, name, tparams, Template(parents, self, body)) = cls
    val body0 = body.map {
      case q"$mods val $name: $tpt[..$args]" => q"$mods val $name: $tpt[$FF, ..$args]"
      case x => x
    }
    val parentTrees = parents ++ List(tq"$FreeModuleLike", tq"freestyle.EffectLike[$FF]")
    ClassDef(mods, name, FFtypeConstructor :: tparams,
      Template(parentTrees, self, body0)).validNel
  }

  private[this] def makeModuleTree(
    cls: ClassDef,
    effects: List[(Name, Tree)]
  ): ValidatedNel[String, ModuleDef] = {

    import cls.{ tparams, name }

    val builtArgs = effects.traverse {
      case (n, tq"$tpt[..$args]") => q"""${n.toTermName}: $tpt[$LL, ..$args]""".validNel
      case (n, t) => s"unexpected type $n: $t when building args".invalidNel
    }

    builtArgs.map(args => q"""
      object ${name.toTermName} {
        val _computeOp = _root_.freestyle.moduleImpl.computeCoproduct
        type Op[$AA] = _computeOp.Op[$AA]
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

object moduleImpl {
  // a nested macro used to delay expansion of the coproduct types until
  // after the free/module high level code is generated
  def computeCoproduct: Any = macro moduleImpl.computeCoproductImpl
}
