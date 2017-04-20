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

import cats._
import cats.implicits._
import org.scalatest.{Matchers, WordSpec}


object taglessAlg {

  @tagless
  trait TG1 {
    def x(a: Int): FS[Int]
    def y(a: Int): FS[Int]
  }

  implicit val optionHandler1: TG1.Handler[Option] = new TG1.Handler[Option] {
    def x(a: Int): Option[Int] = Some(a)
    def y(a: Int): Option[Int] = Some(a)
  }
}

class taglessTests extends WordSpec with Matchers {

  import taglessAlg._


  "the @tagless annotation" should {

    "compile" in {

      import freestyle.implicits._

      def program[F[_]] = {
        val s = TG1.Ops
        for {
          a <- s.x(1)
          b <- s.y(1)
        } yield a + b
      }
      program.exec[Option] shouldBe Option(2)

    }

  }

}
