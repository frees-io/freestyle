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

import org.scalatest.{Matchers, WordSpec}
import cats.implicits._

class tests extends WordSpec with Matchers {

  "Presentation Compiler Support" should {

    "generate code that works in the presentation compiler" in {
      import org.ensime.pcplod._
      withMrPlod("pcplodtest.scala") { pc =>
        pc.typeAtPoint('result) shouldBe Some("Option[Int]")
        // Disabled test: the name of the parameter of `FreeS` is no longer known.
        //pc.typeAtPoint('test) shouldBe Some("(n: Int)freestyle.FreeS[F,Int]")
        pc.typeAtPoint('handler) shouldBe Some("pcplodtest.PcplodTestAlgebra.Handler")
        pc.messages should be a 'empty
      }
    }

  }

}
