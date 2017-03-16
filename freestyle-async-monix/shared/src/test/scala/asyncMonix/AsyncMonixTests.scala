package freestyle.asyncMonix

import org.scalatest._

import freestyle._
import freestyle.implicits._
import freestyle.async._
import freestyle.async.implicits._
import freestyle.asyncMonix.implicits._

import monix.eval.Task
import monix.cats._
import monix.execution.Scheduler

class AsyncMonixTests extends AsyncWordSpec with Matchers {
  implicit override def executionContext = Scheduler.Implicits.global

  "Async Monix Freestyle integration" should {
    "support Task as the target runtime" in {
      def program[F[_]: AsyncM.To] =
        for {
          a <- FreeS.pure(1)
          b <- AsyncM[F].async[Int]((cb) => cb(Right(42)))
          c <- FreeS.pure(1)
          d <- AsyncM[F].async[Int]((cb) => {
            Thread.sleep(100)
            cb(Right(10))
          })
        } yield a + b + c + d

      program[AsyncM.Op].exec[Task].runAsync map { _ shouldBe 54 }
    }
  }
}
