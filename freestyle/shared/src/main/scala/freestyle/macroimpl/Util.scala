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

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.Defn.{ Class, Trait, Object }

/* Utilities for scalameta independents of freestyle*/
private[this] object ScalametaUtil {

  def isAbstract(cls: Class): Boolean = cls.mods.exists {
    case Mod.Abstract() => true
    case _ => false
  }

  def toVar(name: Term.Name) = Pat.Var.Term(name)

  def toName(par: Term.Param): Term.Name = Term.Name(par.name.value)

  def toType(par: Type.Param): Type = Type.Name(par.name.value)

  def tparam(tyn: Type.Name): Type.Param =
    q"type X[Y]".tparams.head.copy(name = tyn) // take Y, replace Y name with tyn

  def tparamK(tyn: Type.Name): Type.Param =
    q"type X[Y[_]]".tparams.head.copy(name = tyn) // take Y[_], replace Y name with Tyn

  /* Given a Decl.Def, that represents an abstract method, make a concrete method Defn.Def
   *   by giving it a Term that serves as its body */
  def addBody(decl: Decl.Def, body: Term): Defn.Def =
    Defn.Def(decl.mods, decl.name, decl.tparams, decl.paramss, Some(decl.decltpe), body)

  def addTypeArg(tyApp: Type, tyArg: Type): Type.Apply = tyApp match {
    case Type.Apply(tyFun, tyArgs) => Type.Apply(tyFun, tyArg +: tyArgs)
    case t => Type.Apply( t, Seq(tyArg) )
  }

  def tyApply(tyFun: Type, tyArg: Type): Type.Apply = Type.Apply(tyFun, Seq(tyArg))

  def mkObject(
    mods: Seq[Mod] = Nil,
    name: Term.Name,
    early: Seq[Stat] = Nil,
    parents: Seq[Ctor.Call] = Nil,
    self: Term.Param = Term.Param(Nil, Name.Anonymous(), None, None),
    stats: Seq[Stat]) = Object( mods, name, Template(early, parents, self, if (stats.isEmpty) Some(stats) else None))
}

