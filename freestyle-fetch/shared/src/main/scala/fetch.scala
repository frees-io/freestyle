package freestyle

import _root_.fetch._

object fetch {

  @free sealed trait FetchM {
    def runA[A](f: Fetch[A]): Oper.Par[A]
    def runF[A](f: Fetch[A]): Oper.Par[(FetchEnv, A)]
    def runE[A](f: Fetch[A]): Oper.Par[FetchEnv]
    def runAWithCache[A](f: Fetch[A], cache: DataSourceCache): Oper.Par[A]
    def runFWithCache[A](f: Fetch[A], cache: DataSourceCache): Oper.Par[(FetchEnv, A)]
    def runEWithCache[A](f: Fetch[A], cache: DataSourceCache): Oper.Par[FetchEnv]
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

    class FetchFreeSLift[F[_]: FetchM.To] extends FreeSLift[F, Fetch] {
      def liftFSPar[A](fetch: Fetch[A]): FreeS.Par[F, A] = FetchM[F].runA(fetch)
    }

    implicit def freeSLiftFetch[F[_]: FetchM.To]: FreeSLift[F, Fetch] = new FetchFreeSLift[F]

  }

}
