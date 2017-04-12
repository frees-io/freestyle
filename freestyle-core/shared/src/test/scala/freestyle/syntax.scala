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

import cats.arrow.FunctionK
import cats.Eval
import org.scalatest.{Matchers, WordSpec}

class liftTests extends WordSpec with Matchers {

  "Lifting syntax" should {

    "allow any value to be lifted into a FreeS monadic context" in {
      import cats.implicits._
      import freestyle.implicits._

      def program[F[_]] =
        for {
          a <- Eval.now(1).freeS
          b <- 2.pure[Eval].freeS
          c <- 3.pure[Eval].freeS
        } yield a + b + c
      implicit val interpreter = FunctionK.id[Eval]
      program[Eval].exec[Eval].value shouldBe 6
    }

  }

}
