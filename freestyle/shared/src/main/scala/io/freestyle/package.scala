package io

import cats.{Monad, RecursiveTailRecM, ~>}
import cats.free.{Free, FreeApplicative}

package object freestyle {

  /**
   * A sequential series of parallel program fragments.
   *
   * `SeqPar` and the relating functions below are a translation into Scala
   * from John De Goes' original gist which can be found at
   * https://gist.github.com/jdegoes/dfaa07042f51245fa09716c6387aa5a6
   */
  type SeqPar[F[_], A] = Free[FreeApplicative[F, ?], A]

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

}