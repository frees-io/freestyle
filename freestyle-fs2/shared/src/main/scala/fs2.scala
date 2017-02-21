package freestyle

import _root_.fs2._
import _root_.fs2.util.{Attempt, Catchable, Free}

import cats._
import cats.free.{Free => CFree}
import cats.arrow.FunctionK

import scala.concurrent._

object fs2 {
  type Eff[A] = Free[CFree[Attempt, ?], A]

  implicit val catsFreeC: Catchable[CFree[Attempt, ?]] = new Catchable[CFree[Attempt, ?]] {
    def pure[A](a: A): CFree[Attempt, A]                              = CFree.pure(a)
    def attempt[A](fa: CFree[Attempt, A]): CFree[Attempt, Attempt[A]] = fa.map(Attempt(_))
    def fail[A](err: Throwable): CFree[Attempt, A]                    = CFree.liftF(Left(err))
    def flatMap[A, B](a: CFree[Attempt, A])(f: A => CFree[Attempt, B]): CFree[Attempt, B] =
      a.flatMap(f)
  }

  implicit val effC: Catchable[Eff] = new Catchable[Eff] {
    def pure[A](a: A): Eff[A]                            = Free.pure(a)
    def attempt[A](fa: Eff[A]): Eff[Attempt[A]]          = fa.attempt
    def fail[A](err: Throwable): Eff[A]                  = Free.fail(err)
    def flatMap[A, B](a: Eff[A])(f: A => Eff[B]): Eff[B] = a.flatMap(f)
  }

  @free sealed trait StreamM[F[_]] {
    def run[A](s: Stream[Eff, A]): FreeS.Par[F, Unit]
    def runLog[A](s: Stream[Eff, A]): FreeS.Par[F, Vector[A]]
    def runFold[A, B](z: B, f: (B, A) => B)(s: Stream[Eff, A]): FreeS.Par[F, B]
    def runLast[A](s: Stream[Eff, A]): FreeS.Par[F, Option[A]]
  }

  object implicits {
    implicit def freeStyleFs2StreamInterpreter[F[_]](
        implicit ME: MonadError[F, Throwable]
    ): StreamM.Interpreter[F] = {
      val attemptF = new FunctionK[Attempt, F] {
        def apply[A](fa: Attempt[A]): F[A] = fa match {
          case Left(err) => ME.raiseError(err)
          case Right(v)  => ME.pure(v)
        }
      }

      new StreamM.Interpreter[F] {
        def runImpl[A](s: Stream[Eff, A]): F[Unit] =
          s.run.run.foldMap(attemptF)

        def runLogImpl[A](s: Stream[Eff, A]): F[Vector[A]] =
          s.runLog.run.foldMap(attemptF)

        def runFoldImpl[A, B](z: B, f: (B, A) => B, s: Stream[Eff, A]): F[B] =
          s.runFold(z)(f).run.foldMap(attemptF)

        def runLastImpl[A](s: Stream[Eff, A]): F[Option[A]] =
          s.runLast.run.foldMap(attemptF)
      }
    }
  }
}
