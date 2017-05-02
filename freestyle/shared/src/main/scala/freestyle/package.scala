/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import cats.free.{Free, FreeApplicative}
import cats.{~>, Applicative, Monad}
import annotation.implicitNotFound

package object freestyle {

  /**
   * A sequential series of parallel program fragments.
   *
   * originally named `SeqPar` and some of the relating functions below originated from a translation into Scala
   * from John De Goes' original gist which can be found at
   * https://gist.github.com/jdegoes/dfaa07042f51245fa09716c6387aa5a6
   */
  type FreeS[F[_], A] = Free[FreeApplicative[F, ?], A]

  @implicitNotFound(msg = AnnotationMessages.handlerNotFoundMsg)
  type FSHandler[F[_], G[_]] = F ~> G

  /** Interprets a parallel fragment `f` into `g` */
  @implicitNotFound(msg = AnnotationMessages.handlerNotFoundMsg)
  type ParInterpreter[F[_], G[_]] = FSHandler[FreeApplicative[F, ?], G]

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
    def liftSeq[F[_], A](free: Free[F, A]): FreeS[F, A] = {
      val lifter = λ[(F ~> FreeS.Par[F, ?])](fa => FreeApplicative.lift(fa))
      free.compile(lifter)
    }

    /** Lift a parallel `FreeApplicative[F, A]` into `FreeS[F, A]` */
    def liftPar[F[_], A](freeap: FreeS.Par[F, A]): FreeS[F, A] =
      Free.liftF(freeap)

    def inject[F[_], G[_]](implicit I: InjK[F, G]): F ~> FreeS.Par[G, ?] =
      λ[F ~> FreeS.Par[G, ?]](fa => FreeApplicative.lift(I.inj(fa) ) )

    /**
     * Lift a pure `A` value `FreeS[F, A]`.
     */
    def pure[F[_], A](a: A): FreeS[F, A] =
      liftPar(FreeApplicative.pure(a))

  }

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
    def parInterpret[H[_]: Monad](implicit interpreter: ParInterpreter[F, H]): H[A] =
      fa.foldMap(interpreter)

    /**
     * Runs a seq/par program by converting each parallel fragment in `f` into an `H`
     * `H` should probably be an `IO`/`Task` like `Monad` also providing parallel execution.
     */
    def interpret[H[_]: Monad](implicit handler: FSHandler[F, H]): H[A] = {
      val parInterpreter = λ[FSHandler[FreeApplicative[F, ?], H]](_.foldMap(handler))
      fa.foldMap(parInterpreter)
    }
  }

  /**
   * Syntax functions for FreeS.Par
   */
  implicit class FreeSParSyntax[F[_], A](private val fa: FreeS.Par[F, A]) extends AnyVal {

    /**
     * Back to sequential computation in the context of FreeS
     */
    def freeS: FreeS[F, A] = FreeS.liftPar(fa)

    def interpret[G[_]: Applicative](implicit handler: FSHandler[F,G]): G[A] =
      fa.foldMap(handler)
  }

  implicit class FreeSLiftSyntax[G[_], A](ga: G[A]) {
    def liftFS[F[_]](implicit L: FreeSLift[F, G]): FreeS[F, A]        = L.liftFS(ga)
    def liftFSPar[F[_]](implicit L: FreeSLift[F, G]): FreeS.Par[F, A] = L.liftFSPar(ga)
  }

  implicit def freeSPar2Seq[F[_], A](fa: FreeS.Par[F, A]): FreeS[F, A] = FreeS.liftPar(fa)

}
