/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.free.internal

import scala.collection.immutable.Seq
import scala.meta.Defn.{Class, Trait, Object}
import scala.meta._

/* Utilities for scalameta independents of freestyle*/
object ScalametaUtil {

  def isAbstract(cls: Class): Boolean = cls.mods.exists {
    case Mod.Abstract() => true
    case _              => false
  }

  def toVar(name: Term.Name)             = Pat.Var.Term(name)

  def tyApply(tyFun: Type, tyArg: Type): Type.Apply = Type.Apply(tyFun, Seq(tyArg))

  def tyAddArg(tyApp: Type, tyArg: Type): Type.Apply = tyApp match {
    case Type.Apply(tyFun, tyArgs) => Type.Apply(tyFun, tyArg +: tyArgs)
    case ty                        => Type.Apply(ty, Seq(tyArg))
  }


  def mkObject(
      mods: Seq[Mod] = Nil,
      name: Term.Name,
      early: Seq[Stat] = Nil,
      parents: Seq[Ctor.Call] = Nil,
      self: Term.Param = Term.Param(Nil, Name.Anonymous(), None, None),
      stats: Seq[Stat]) =
    Object(mods, name, Template(early, parents, self, if (stats.isEmpty) Some(stats) else None))

  implicit class ModsOps(val mods: Seq[Mod]) extends AnyVal {

    def hasMod(mod: Mod): Boolean = mods.exists {
      case `mod` => true
      case _ => false
    }

    def removeMod(mod: Mod): Seq[Mod] = mods.filter {
      case `mod` => false
      case _ => true
    }

    def filtered: Seq[Mod] = mods.filter {
      case mod"@debug" => false
      case _           => true
    }

    def isDebug: Boolean = mods exists {
      case mod"@debug" => true
      case _           => false
    }

  }

  implicit class TypeOps(val theType: Type) extends AnyVal {
    def applyTo(tparams: Seq[Type]): Type =
      if (tparams.isEmpty) theType else Type.Apply(theType, tparams)
  }

  implicit class TypeNameOps(val typeName: Type.Name) extends AnyVal {

    // take Y, replace Y name with tyn
    def param: Type.Param = q"type X[Y]".tparams.head.copy(name = typeName)

    // take Y[_], replace Y name with Tyn
    def paramK: Type.Param = q"type X[Y[_]]".tparams.head.copy(name = typeName)

    def ctor: Ctor.Ref.Name = Ctor.Ref.Name(typeName.value)

    def term: Term.Name = Term.Name(typeName.value)
  }

  implicit class TermParamOps(val termParam: Term.Param) extends AnyVal {
    def addMod(mod: Mod): Term.Param = termParam.copy(mods = termParam.mods :+ mod)

    def toName: Term.Name = Term.Name(termParam.name.value)

    def isImplicit: Boolean = termParam.mods.exists {
      case Mod.Implicit() => true
      case _ => false
    }

  }

  implicit class TermNameOps(val termName: Term.Name) extends AnyVal {
    def toVar = Pat.Var.Term.apply(termName)
    def param: Term.Param = Term.Param( Nil, termName, None, None)
    def ctor: Ctor.Ref.Name = Ctor.Ref.Name(termName.value)
  }

  implicit class TypeParamOps(val typeParam: Type.Param) extends AnyVal {
    def toName: Type.Name = Type.Name(typeParam.name.value)

    def classBoundsToParamTypes: Seq[Type.Apply] = typeParam.cbounds.map { cbound =>
      Type.Apply(cbound, Seq(Type.Name(typeParam.name.value)))
    }

    def isKind1: Boolean =
      typeParam.tparams.toList match {
        case List(tp) if tp.tparams.isEmpty => true
        case _ => false
      }

    def unboundC: Type.Param = typeParam.copy(cbounds = Nil)
  }

  implicit class DeclDefOps(val declDef: Decl.Def) extends AnyVal {

    def addMod(mod: Mod): Decl.Def = declDef.copy(mods = declDef.mods :+ mod)

    def argss: Seq[Seq[Term.Name]] = declDef.paramss.map(_.map(_.toName))

    def withType(ty: Type) = declDef.copy(decltpe = ty)

    /* Build a concrete method Defn.Def by giving the body Term */
    def addBody(body: Term): Defn.Def = {
      import declDef._
      Defn.Def(mods, name, tparams, paramss, Some(decltpe), body)
    }

    def hasImplicitParams: Boolean =
      declDef.paramss.lastOption.exists( _.exists { (param: Term.Param) =>
        param.mods.exists {
          case Mod.Implicit() => true
          case _ => false
        }
      })
  }

  implicit class DefnDefOps(val defnDef: Defn.Def) extends AnyVal {
    def addMod(mod: Mod): Defn.Def = defnDef.copy(mods = defnDef.mods :+ mod)
  }


  implicit class TermParamListOps(val termParams: Seq[Term.Param]) extends AnyVal {
    def hasImplicit: Boolean = termParams.exists(_.isImplicit)

    def toImplicit: Seq[Term.Param] = termParams.toList match {
      case Nil => Nil
      case headParam :: tailParams => headParam.addMod( Mod.Implicit() ) :: tailParams
    }

    def toVals: Seq[Term.Param] = termParams.map(_.addMod(Mod.ValParam()))
  }

  implicit class TermParamListListOps(val termParamss: Seq[Seq[Term.Param]]) extends AnyVal {

    /** Adds the given implicitPars to the list of lists of parameters: if termParamss already
      has an implicits params list, it appends implicitPars to it. Otherwise, it adds a new list*/
    def addImplicits(implicitPars: Seq[Term.Param]): Seq[Seq[Term.Param]] =
      if (implicitPars.isEmpty)
        termParamss
      else if (termParamss.isEmpty)
        Seq(implicitPars.toImplicit)
      else {
        val lastPars = termParamss.last
        if (lastPars.hasImplicit)
          termParamss.init ++ Seq(lastPars ++ implicitPars)
        else
          termParamss ++ Seq( implicitPars.toImplicit)
      }

    def addEmptyExplicit: Seq[Seq[Term.Param]] = termParamss.toList match {
      case Nil => Seq( Seq())
      case List(imps) if imps.hasImplicit => Seq( Seq(), imps)
      case _ => termParamss
    }

  }

  implicit class TemplateOps(val templ: Template) extends AnyVal {

    def addParent( ctorCall: Ctor.Call): Template =
      templ.copy( parents = templ.parents ++ Seq(ctorCall) )

    def addStats( newStats: Seq[Stat]): Template =
      if (newStats.isEmpty) templ else templ.copy(
        stats = Some( templ.stats.getOrElse(Seq()) ++ newStats)
      )
  }

  implicit class ObjectOps(val obj: Object) extends AnyVal {

    def appendStats(newStats: Seq[Stat]): Object =
      if (newStats.isEmpty) obj
      else obj.copy( templ = obj.templ.addStats(newStats))

  }

}
