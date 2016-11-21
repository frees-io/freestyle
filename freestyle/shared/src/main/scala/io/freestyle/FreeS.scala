package io.freestyle

import cats.free.{FreeApplicative, Free, Inject}
import cats._
import cats.arrow.FunctionK
import cats.data.Coproduct

trait FreeSDefinitions {

  /**
   * A sequential series of parallel program fragments.
   *
   * originally named `SeqPar` and some of the relating functions below originated from a translation into Scala
   * from John De Goes' original gist which can be found at
   * https://gist.github.com/jdegoes/dfaa07042f51245fa09716c6387aa5a6
   */
  type FreeS[F[_], A] = Free[FreeApplicative[F, ?], A]

  /** Interprets a parallel fragment `f` into `g` */
  type ParInterpreter[F[_], G[_]] = FreeApplicative[F, ?] ~> G

  /**
   * Optimizes a parallel fragment `f` into a sequential series of parallel
   * program fragments in `g`.
   */
  type ParOptimizer[F[_], G[_]] = ParInterpreter[F, FreeS[G, ?]]

  object FreeS {

    type Par[F[_], A] = FreeApplicative[F, A]

    /** Lift an `F[A]` value into `FreeS[F, A]` */
    def liftFA[F[_], A](fa: F[A]): FreeS[F, A] =
      Free.liftF(FreeApplicative.lift(fa))

    /** Lift a sequential `Free[F, A]` into `FreeS[F, A]` */
    def liftSeq[F[_], A](free: Free[F, A]): FreeS[F, A] =
      free.compile(λ[(F ~> FreeApplicative[F, ?])](fa => FreeApplicative.lift(fa)))

    /** Lift a parallel `FreeApplicative[F, A]` into `FreeS[F, A]` */
    def liftPar[F[_], A](freeap: FreeApplicative[F, A]): FreeS[F, A] =
      Free.liftF(freeap)

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

}
