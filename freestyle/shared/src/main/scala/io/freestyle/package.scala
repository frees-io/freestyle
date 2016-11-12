package io

import cats.{Monad, RecursiveTailRecM, ~>}
import cats.free.{Free, FreeApplicative}

package object freestyle {

  /**
   * A sequential series of parallel program fragments.
   *
   * originally named `SeqPar` and the relating functions below originated from a translation into Scala
   * from John De Goes' original gist which can be found at
   * https://gist.github.com/jdegoes/dfaa07042f51245fa09716c6387aa5a6
   */
  type SeqPar[F[_], A] = Free[FreeApplicative[F, ?], A]

  type FreeS[F[_], A] = SeqPar[F, A]

  object FreeS {
    type Par[F[_], A] = FreeApplicative[F, A]
  }

  object SeqPar {

    /** Lift an `F[A]` value into `SeqPar[F, A]` */
    def liftFA[F[_], A](fa: F[A]): SeqPar[F, A] =
      Free.liftF(FreeApplicative.lift(fa))

    /** Lift a sequential `Free[F, A]` into `SeqPar[F, A]` */
    def liftSeq[F[_], A](free: Free[F, A]): SeqPar[F, A] =
      free.compile(λ[(F ~> FreeApplicative[F, ?])](fa => FreeApplicative.lift(fa)))

    /** Lift a parallel `FreeApplicative[F, A]` into `SeqPar[F, A]` */
    def liftPar[F[_], A](freeap: FreeApplicative[F, A]): SeqPar[F, A] =
      Free.liftF(freeap)
  }

  /** Interprets a parallel fragment `f` into `g` */
  type ParInterpreter[F[_], G[_]] = FreeApplicative[F, ?] ~> G

  /**
   * Optimizes a parallel fragment `f` into a sequential series of parallel
   * program fragments in `g`.
   */
  type ParOptimizer[F[_], G[_]] = ParInterpreter[F, SeqPar[G, ?]]

  /** This may eventually make it to cats once we have a solid impl */
  object FreeApplicativeExt {

    import cats.free._

    def inject[F[_], G[_]]: FreeApInjectPartiallyApplied[F, G] =
      new FreeApInjectPartiallyApplied

    /**
     * Pre-application of an injection to a `F[A]` value.
     */
    final class FreeApInjectPartiallyApplied[F[_], G[_]] {
      def apply[A](fa: F[A])(implicit I: Inject[F, G]): FreeApplicative[G, A] =
        FreeApplicative.lift(I.inj(fa))
    }

  }

  object implicits {

    import cats._
    import cats.arrow.FunctionK
    import cats.data.Coproduct

    // Syntax functions on `SeqPar`
    implicit class SeqParOps[F[_], A](private val seqpar: SeqPar[F, A]) extends AnyVal {

      /**
       * Applies the most general optimization from a parallel program fragment
       * in `f` to a sequential.
       */
      def optimize[G[_]](opt: ParOptimizer[F, G]): SeqPar[G, A] =
        seqpar.foldMap(opt)

      /** Applies a parallel-to-parallel optimization */
      def parOptimize[G[_]](opt: ParInterpreter[F, FreeApplicative[G, ?]]): SeqPar[G, A] =
        seqpar.compile(opt)

      /**
       * Runs a seq/par program by converting each parallel fragment in `f` into an `H`
       * `H` should probably be an `IO`/`Task` like `Monad` also providing parallel execution.
       */
      def run[H[_]: Monad: RecursiveTailRecM](f: ParInterpreter[F, H]): H[A] =
        seqpar.foldMap(f)
    }

    /**
     * Syntax functions for FreeApplicative
     */
    implicit class ParLift[F[_], A](private val dp: FreeApplicative[F, A]) extends AnyVal {

      /**
       * Back to sequential computation in the context of SeqPar
       */
      def seq: SeqPar[F, A] = SeqPar.liftPar(dp)

      def exec[G[_]: Applicative](implicit interpreter: FunctionK[F, G]): G[A] =
        dp.foldMap(interpreter)
    }

    implicit class SeqParExecSyntax[F[_], A](fa: SeqPar[F, A]) {
      def exec[G[_]: Monad: RecursiveTailRecM](implicit interpreter: FunctionK[FreeApplicative[F, ?], G]): G[A] =
        fa.foldMap(interpreter)
    }

    implicit def interpretCoproduct[F[_], G[_], M[_]](implicit fm: FunctionK[F, M], gm: FunctionK[G, M]): FunctionK[Coproduct[F, G, ?], M] =
      fm or gm

    implicit def interpretAp[F[_], M[_]: Monad: RecursiveTailRecM](implicit freeInterpreter: FunctionK[F, M]): FunctionK[FreeApplicative[F, ?], M] =
      new cats.arrow.FunctionK[FreeApplicative[F, ?], M] {
        override def apply[A](fa: FreeApplicative[F, A]): M[A] = fa match {
          case x @ _ => x.foldMap(freeInterpreter)
        }
      }

    object parallel {

      import scala.concurrent.Future
      import scala.concurrent.ExecutionContext
      import scala.util.control.NonFatal

      implicit def freestyleParallelFutureMonad(implicit ec: ExecutionContext): Monad[Future] =
        new Monad[Future] {

          def pure[A](x: A): Future[A] = Future.successful(x)

          def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

          final def tailRecM[B, C](b: B)(f: B => Future[Either[B, C]]): Future[C] =
            f(b).flatMap {
              case Left(b1) => tailRecM(b1)(f)
              case Right(c) => Future.successful(c)
            }

          override def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

          override def ap[A, B](ff: Future[A => B])(fa: Future[A]): Future[B] =
            fa.zip(ff).map { case (a, f) => f(a) }

        }
    }
  }

}
