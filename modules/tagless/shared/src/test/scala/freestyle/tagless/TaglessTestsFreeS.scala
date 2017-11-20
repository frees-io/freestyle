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

package freestyle.tagless
package puretestImpl

import org.hablapps.puretest._
import cats.~>
import cats.instances.either._

import TaglessTestsFreeS._

class TaglessTestsFreeS extends TaglessTests.ScalaTest[P](a1,a2,a3)(implicitly, HE,RE,T)

object TaglessTestsFreeS{

  /** TG1, TG2, TG3 */ 

  import freestyle._
  import freestyle.implicits._
  import algebras._
  import modules._

  import freestyle.effects.either
  val e = either[PuretestError[String]]
  import e.implicits._

  @module trait App {
    val tg1: TG1.StackSafe
    val tg2: TG2.StackSafe
    val tg3: TG3.StackSafe
    val te: e.EitherM
  }

  type P[T]=FreeS[App.Op,T]

  // `val a1 = TG1[P]` fails to compile (diverging implicit expansion)
  val a1 = new TG1.Handler[P] {
    def x(a: Int) = TG1.StackSafe.to[App.Op].x(a)
    def y(a: Int) = TG1.StackSafe.to[App.Op].y(a)
  }

  val a2 = new TG2.Handler[P] {
    def x2(a: Int): FS[Int] = TG2.StackSafe.to[App.Op].x2(a)
    def y2(a: Int): FS[Int] = TG2.StackSafe.to[App.Op].y2(a)
  }

  val a3 = new TG3.Handler[P] {
    def x3(a: Int): FS[Int] = TG3.StackSafe.to[App.Op].x3(a)
    def y3(a: Int): FS[Int] = TG3.StackSafe.to[App.Op].y3(a)
  }

  /** Handle Error */

  implicit val HE = new HandleError[P,String]{
    // No need to handle errors in this particular case
    def handleError[T](p: P[T])(f: String => P[T]) = p
  }

  /** Raise Error */

  implicit val RE = new RaiseError[P,PuretestError[String]]{
    def raiseError[A](err: PuretestError[String]) =
      e.EitherM.to[App.Op].error[A](err)
  }

  /** Tester */
  
  // Couldn't get TG1.Handler[Either[...]] from TG1[Either[...]]
  implicit val a1pte: TG1.Handler[Either[PuretestError[String],?]] =
    new TG1.Handler[Either[PuretestError[String],?]] {
      def x(a: Int) = Right(a)
      def y(a: Int) = Right(a)
    }

  implicit val a2pte: TG2.Handler[Either[PuretestError[String],?]] =
    new TG2.Handler[Either[PuretestError[String],?]] {
      def x2(a: Int): FS[Int] = Right(a)
      def y2(a: Int): FS[Int] = Right(a)
    }

  implicit val a3pte: TG3.Handler[Either[PuretestError[String],?]] =
    new TG3.Handler[Either[PuretestError[String],?]] {
      def x3(a: Int): FS[Int] = Right(a)
      def y3(a: Int): FS[Int] = Right(a)
    }

  implicit val T = new Tester[P,PuretestError[String]]{
    def apply[T](p: P[T]): Either[PuretestError[String],T] = 
      p.interpret[Either[PuretestError[String],?]]
  }
}