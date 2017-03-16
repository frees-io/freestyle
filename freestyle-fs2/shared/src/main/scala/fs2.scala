package freestyle

import _root_.fs2._
import _root_.fs2.util.{Attempt, Catchable, Suspendable, Monad, Free}

import cats.{MonadError, Monoid, ~>}
import cats.free.{Free => CFree}

object fs2 {
  type Eff[A] = Free[CFree[Attempt, ?], A]

  implicit val catsFreeAttemptCatchable: Catchable[CFree[Attempt, ?]] = new Catchable[CFree[Attempt, ?]] {
    def pure[A](a: A): CFree[Attempt, A]                              = CFree.pure(a)
    def attempt[A](fa: CFree[Attempt, A]): CFree[Attempt, Attempt[A]] = fa.map(Attempt(_))
    def fail[A](err: Throwable): CFree[Attempt, A]                    = CFree.liftF(Left(err))
    def flatMap[A, B](a: CFree[Attempt, A])(f: A => CFree[Attempt, B]): CFree[Attempt, B] =
      a.flatMap(f)
  }

  private[fs2] sealed class EffMonad extends Monad[Eff] {
    def pure[A](a: A): Eff[A]                            = Free.pure(a)
    def flatMap[A, B](a: Eff[A])(f: A => Eff[B]): Eff[B] = a.flatMap(f)
  }

  implicit val effCatchable: Catchable[Eff] = new EffMonad with Catchable[Eff] {
    def attempt[A](fa: Eff[A]): Eff[Attempt[A]] = fa.attempt
    def fail[A](err: Throwable): Eff[A]         = Free.fail(err)
  }

  implicit val effSuspendable: Suspendable[Eff] = new EffMonad with Suspendable[Eff] {
    def suspend[A](fa: => Eff[A]): Eff[A] = fa
  }

  @free sealed trait StreamM {
    def run[A](s: Stream[Eff, A]): Oper.Par[Unit]
    def runLog[A](s: Stream[Eff, A]): Oper.Par[Vector[A]]
    def runFold[A, B](z: B, f: (B, A) => B)(s: Stream[Eff, A]): Oper.Par[B]
    def runLast[A](s: Stream[Eff, A]): Oper.Par[Option[A]]
  }

  object implicits {
    implicit def freeStyleFs2StreamHandler[F[_]](
        implicit ME: MonadError[F, Throwable]
    ): StreamM.Handler[F] = {
      val attemptF = λ[Attempt ~> F](_.fold(ME.raiseError, ME.pure))

      new StreamM.Handler[F] {
        def run[A](s: Stream[Eff, A]): F[Unit] =
          s.run.run.foldMap(attemptF)

        def runLog[A](s: Stream[Eff, A]): F[Vector[A]] =
          s.runLog.run.foldMap(attemptF)

        def runFold[A, B](z: B, f: (B, A) => B, s: Stream[Eff, A]): F[B] =
          s.runFold(z)(f).run.foldMap(attemptF)

        def runLast[A](s: Stream[Eff, A]): F[Option[A]] =
          s.runLast.run.foldMap(attemptF)
      }
    }

    implicit class Fs2FreeSyntax[A](private val s: Stream[Eff, A]) extends AnyVal {
      def liftFS[F[_]](implicit MA: Monoid[A], SF: StreamM.To[F]): FreeS[F, A] =
        liftFSPar.freeS

      def liftFSPar[F[_]](implicit MA: Monoid[A], SF: StreamM.To[F]): FreeS.Par[F, A] =
        SF.runFold(MA.empty, MA.combine)(s)
    }
  }
}
