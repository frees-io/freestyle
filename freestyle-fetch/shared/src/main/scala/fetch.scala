package io.freestyle

import cats.data.NonEmptyList
import cats.~>
import cats.instances.list._
import cats.data.State
import _root_.fetch._

import io.freestyle._
import cats.free.FreeApplicative

object fetch {

  @free sealed trait FetchM[F[_]] {
    def runA[A](f: Fetch[A]): FreeS.Par[F, A]
    def runF[A](f: Fetch[A]): FreeS.Par[F, (FetchEnv, A)]
    def runE[A](f: Fetch[A]): FreeS.Par[F, FetchEnv]
    def runAWithCache[A](f: Fetch[A], cache: DataSourceCache): FreeS.Par[F, A]
    def runFWithCache[A](f: Fetch[A], cache: DataSourceCache): FreeS.Par[F, (FetchEnv, A)]
    def runEWithCache[A](f: Fetch[A], cache: DataSourceCache): FreeS.Par[F, FetchEnv]
  }

  object implicits {

    implicit def freeStyleFetchInterpreter[M[_]: FetchMonadError]: FetchM.Interpreter[M] = new FetchM.Interpreter[M] {
      import _root_.fetch.syntax._
      def runAImpl[A](fa: Fetch[A]): M[A] = fa.runA[M]
      def runFImpl[A](fa: Fetch[A]): M[(FetchEnv,A)] = fa.runF[M]
      def runEImpl[A](fa: Fetch[A]): M[FetchEnv] = fa.runE[M]
      def runAWithCacheImpl[A](fa: Fetch[A], cache: DataSourceCache): M[A] = fa.runA[M](cache)
      def runFWithCacheImpl[A](fa: Fetch[A], cache: DataSourceCache): M[(FetchEnv,A)] = fa.runF[M](cache)
      def runEWithCacheImpl[A](fa: Fetch[A], cache: DataSourceCache): M[FetchEnv] = fa.runE[M](cache)
    }

    class FetchFreeSLift[F[_]: FetchM] extends FreeSLift[F, Fetch] {
      def liftFSPar[A](fetch: Fetch[A]): FreeS.Par[F, A] = FetchM[F].runA(fetch)
    }

    implicit def freeSLiftFetch[F[_]: FetchM]: FreeSLift[F, Fetch] = new FetchFreeSLift[F]

  }

}

