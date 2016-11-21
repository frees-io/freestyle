package io

import cats.{Applicative, Monad, ~>}
import cats.free.{Free, FreeApplicative}

package object freestyle extends FreeSDefinitions {

    // Syntax functions on `FreeS`
    implicit class FreeSOps[F[_], A](private val seqpar: FreeS[F, A]) extends AnyVal {

      /**
       * Applies the most general optimization from a parallel program fragment
       * in `f` to a sequential.
       */
      def optimize[G[_]](opt: ParOptimizer[F, G]): FreeS[G, A] =
        seqpar.foldMap(opt)

      /** Applies a parallel-to-parallel optimization */
      def parOptimize[G[_]](opt: ParInterpreter[F, FreeApplicative[G, ?]]): FreeS[G, A] =
        seqpar.compile(opt)

      /**
       * Runs a seq/par program by converting each parallel fragment in `f` into an `H`
       * `H` should probably be an `IO`/`Task` like `Monad` also providing parallel execution.
       */
      def exec[H[_]: Monad](implicit interpreter: ParInterpreter[F, H]): H[A] =
        seqpar.foldMap(interpreter)
    }


   /**
     * Syntax functions for FreeApplicative
     */
    implicit class ParLift[F[_], A](private val dp: FreeApplicative[F, A]) extends AnyVal {

      /**
       * Back to sequential computation in the context of FreeS
       */
      def seq: FreeS[F, A] = FreeS.liftPar(dp)

      def exec[G[_]: Applicative](implicit interpreter: F ~> G): G[A] =
        dp.foldMap(interpreter)
    }

  object implicits extends Interpreters


}

