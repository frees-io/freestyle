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

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._

import algebras._

trait TaglessTests[F[_]] extends FunSpec[F] {

  implicit val HE: HandleError[F,String]
  implicit val RE: RaiseError[F, PuretestError[String]]
  implicit val M: Monad[F]
  
  implicit val a1: TG1[F]
  implicit val a2: TG2[F]
  implicit val a3: TG3[F]
  
  Describe("Tagless final algebras"){

    It("should combine with other tagless algebras"){
      (for {
        a <- a1.x(1)
        b <- a1.y(1)
        c <- a2.x2(1)
        d <- a2.y2(1)
        e <- a3.y3(1)
      } yield a + b + c + d + e) shouldBe 5
    }

    It("should work with derived handlers"){
      (for {
        x <- TG1[F].x(1)
        y <- TG1[F].y(2)
      } yield x + y) shouldBe 3
    }
  }
}

object TaglessTests{
  class ScalaTest[F[_]](
    val a1: TG1[F],
    val a2: TG2[F],
    val a3: TG3[F])(implicit 
    val M: Monad[F],
    val HE: HandleError[F,String],
    val RE: RaiseError[F,PuretestError[String]],
    val Tester: Tester[F,PuretestError[String]]
  ) extends scalatestImpl.FunSpec[F,String] with TaglessTests[F]
}