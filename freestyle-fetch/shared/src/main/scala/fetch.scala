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

package freestyle

import _root_.fetch._

object fetch {

  @free sealed trait FetchM {
    def runA[A](f: Fetch[A]): OpPar[A]
    def runF[A](f: Fetch[A]): OpPar[(FetchEnv, A)]
    def runE[A](f: Fetch[A]): OpPar[FetchEnv]
    def runAWithCache[A](f: Fetch[A], cache: DataSourceCache): OpPar[A]
    def runFWithCache[A](f: Fetch[A], cache: DataSourceCache): OpPar[(FetchEnv, A)]
    def runEWithCache[A](f: Fetch[A], cache: DataSourceCache): OpPar[FetchEnv]
  }

  object implicits {

    implicit def freeStyleFetchHandler[M[_]: FetchMonadError]: FetchM.Handler[M] =
      new FetchM.Handler[M] {
        import _root_.fetch.syntax._
        def runA[A](fa: Fetch[A]): M[A]                                  = fa.runA[M]
        def runF[A](fa: Fetch[A]): M[(FetchEnv, A)]                      = fa.runF[M]
        def runE[A](fa: Fetch[A]): M[FetchEnv]                           = fa.runE[M]
        def runAWithCache[A](fa: Fetch[A], cache: DataSourceCache): M[A] = fa.runA[M](cache)
        def runFWithCache[A](fa: Fetch[A], cache: DataSourceCache): M[(FetchEnv, A)] =
          fa.runF[M](cache)
        def runEWithCache[A](fa: Fetch[A], cache: DataSourceCache): M[FetchEnv] =
          fa.runE[M](cache)
      }

    class FetchFreeSLift[F[_]: FetchM] extends FreeSLift[F, Fetch] {
      def liftFSPar[A](fetch: Fetch[A]): FreeS.Par[F, A] = FetchM[F].runA(fetch)
    }

    implicit def freeSLiftFetch[F[_]: FetchM]: FreeSLift[F, Fetch] = new FetchFreeSLift[F]

  }

}
