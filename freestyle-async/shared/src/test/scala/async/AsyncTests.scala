package freestyle

import cats.Applicative
import freestyle.async._
import freestyle.async.implicits._
import freestyle.implicits._
import org.scalatest._
import scala.concurrent._

class AsyncTests extends AsyncWordSpec with Matchers {
  implicit override def executionContext = ExecutionContext.Implicits.global

  "Async Freestyle integration" should {
    "allow an Async to be interleaved inside a program monadic flow" in {
      import cats.implicits._
      def program[F[_]: AsyncM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- AsyncM[F].async[Int]((cb) => cb(Right(42)))
          c <- Applicative[FreeS[F, ?]].pure(1)
        } yield a + b + c

      program[AsyncM.T].exec[Future] map { _ shouldBe 44 }
    }

    "allow multiple Async to be interleaved inside a program monadic flow" in {
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

      program[AsyncM.T].exec[Future] map { _ shouldBe 54 }
    }

    case class OhNoException() extends Exception

    "allow Async errors to short-circuit a program" in {
      import cats.implicits._
      def program[F[_]: AsyncM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- AsyncM[F].async[Int]((cb) => cb(Left(OhNoException())))
          c <- Applicative[FreeS[F, ?]].pure(3)
        } yield a + b + c

      program[AsyncM.T].exec[Future] recover { case OhNoException() => 42 } map { _ shouldBe 42 }
    }
  }
}
