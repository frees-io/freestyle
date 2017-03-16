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
