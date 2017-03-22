package freestyle

import org.scalatest.{Matchers, WordSpec}
import cats.implicits._

class tests extends WordSpec with Matchers {

  "Presentation Compiler Support" should {

    "generate code that works in the presentation compiler" in {
      import org.ensime.pcplod._
      withMrPlod("pcplodtest.scala") { pc =>
        pc.typeAtPoint('result) shouldBe Some("Option[Int]")
        pc.typeAtPoint('test) shouldBe Some("(n: Int)freestyle.FreeS[F,Int]")
        pc.typeAtPoint('handler) shouldBe Some("pcplodtest.PcplodTestAlgebra.Handler")
        pc.messages should be a 'empty
      }
    }

  }

}
