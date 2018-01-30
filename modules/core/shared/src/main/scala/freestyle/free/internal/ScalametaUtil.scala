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
  def toName(par: Term.Param): Term.Name = Term.Name(par.name.value)
  def toType(par: Type.Param): Type      = Type.Name(par.name.value)

  def tyParam(ty: Type.Name): Type.Param =
    q"type X[Y]".tparams.head.copy(name = ty) // take Y, replace Y name with tyn

  def tyParamK(ty: Type.Name): Type.Param =
    q"type X[Y[_]]".tparams.head.copy(name = ty) // take Y[_], replace Y name with Tyn

  def tyApply(tyFun: Type, tyArg: Type): Type.Apply = Type.Apply(tyFun, Seq(tyArg))

  def tyAddArg(tyApp: Type, tyArg: Type): Type.Apply = tyApp match {
    case Type.Apply(tyFun, tyArgs) => Type.Apply(tyFun, tyArg +: tyArgs)
    case ty                        => Type.Apply(ty, Seq(tyArg))
  }

  /* Given a Decl.Def, that represents an abstract method, make a concrete method Defn.Def
   *   by giving it a Term that serves as its body */
  def addBody(decl: Decl.Def, body: Term): Defn.Def =
    Defn.Def(decl.mods, decl.name, decl.tparams, decl.paramss, Some(decl.decltpe), body)

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
  }

  implicit class DeclDefOps(val declDef: Decl.Def) extends AnyVal {

    def addMod(mod: Mod): Decl.Def = declDef.copy(mods = declDef.mods :+ mod)

    def argss: Seq[Seq[Term.Name]] = declDef.paramss.map(_.map(toName))

    def withType(ty: Type) = declDef.copy(decltpe = ty)

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

  implicit class TermParamListOps(val termParams: Seq[Term.Param]) extends AnyVal {
    def hasImplicit: Boolean = termParams.exists(_.isImplicit)

    def toImplicit: Seq[Term.Param] = termParams.toList match {
      case Nil => Nil
      case headParam :: tailParams => headParam.addMod( Mod.Implicit() ) :: tailParams
    }
  }
}
