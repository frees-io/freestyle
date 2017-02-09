package freestyle.asyncFs2

import cats.{Applicative, Eval, MonadFilter}

import org.scalatest._

import freestyle._
import freestyle.implicits._
import freestyle.async._
import freestyle.async.implicits._

import scala.concurrent._
import scala.concurrent.duration._

import freestyle.asyncFs2.implicits._
import fs2.{Strategy, Task}
import fs2.interop.cats._

class AsyncFs2Tests extends AsyncWordSpec with Matchers {
  implicit val strategy = Strategy.fromExecutionContext(ExecutionContext.Implicits.global)

  "Async Monix Freestyle integration" should {
    "support Task as the target runtime" in {
      import cats.implicits._
      def program[F[_]: AsyncM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- AsyncM[F].async[Int]((cb) => cb(Right(42)))
          c <- Applicative[FreeS[F, ?]].pure(1)
          d <- AsyncM[F].async[Int]((cb) => {
            Thread.sleep(100)
            cb(Right(10))
          })
        } yield a + b + c + d

      program[AsyncM.T].exec[Task].unsafeRunAsyncFuture map { _ shouldBe 54 }
    }
  }
}
