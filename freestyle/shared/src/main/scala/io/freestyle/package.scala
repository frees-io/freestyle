package io

import cats.{Applicative, Monad, ~>}

package object freestyle extends FreeSDefinitions {

  /**
   * Syntax functions for FreeS.Par
   */
  implicit class FreeSOps[F[_], A](private val fa: FreeS[F, A]) extends AnyVal {

    /**
     * Applies the most general optimization from a parallel program fragment
     * in `f` to a sequential.
     */
    def optimize[G[_]](opt: ParOptimizer[F, G]): FreeS[G, A] =
      fa.foldMap(opt)

    /** Applies a parallel-to-parallel optimization */
    def parOptimize[G[_]](opt: ParInterpreter[F, FreeS.Par[G, ?]]): FreeS[G, A] =
      fa.compile(opt)

    /**
     * Runs a seq/par program by converting each parallel fragment in `f` into an `H`
     * `H` should probably be an `IO`/`Task` like `Monad` also providing parallel execution.
     */
    def exec[H[_]: Monad](implicit interpreter: ParInterpreter[F, H]): H[A] =
      fa.foldMap(interpreter)
  }

  /**
   * Syntax functions for FreeS.Par
   */
  implicit class FreeSParSyntax[F[_], A](private val fa: FreeS.Par[F, A]) extends AnyVal {

    /**
     * Back to sequential computation in the context of FreeS
     */
    def freeS: FreeS[F, A] = FreeS.liftPar(fa)

    def exec[G[_]: Applicative](implicit interpreter: F ~> G): G[A] =
      fa.foldMap(interpreter)
  }

  /**
   * Syntax functions for any F[A]
   */
  implicit class ParLift[F[_], A](private val fa: F[A]) extends AnyVal {

    /**
     * Lift an F[A] into a FreeS[F, A]
     */
    def freeS: FreeS[F, A] = FreeS.liftFA(fa)

  }

  implicit class FreeSLiftSyntax[G[_], A](ga: G[A]) {
    def liftFS[F[_]](implicit L: FreeSLift[F, G]): FreeS[F, A] = L.liftFS(ga)
    def liftFSPar[F[_]](implicit L: FreeSLift[F, G]): FreeS.Par[F, A] = L.liftFSPar(ga)
  }

  implicit def freeSPar2FreeSMonad[F[_], A](fa: FreeS.Par[F, A]): FreeS[F, A] = fa.freeS

}
