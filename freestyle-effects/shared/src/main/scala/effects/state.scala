package freestyle
package effects

import cats.MonadState

object state {

  final class StateSeedProvider[S] {

    @free sealed abstract class StateM[F[_]] {
      def get: FreeS.Par[F, S]
      def set(s: S): FreeS.Par[F, Unit]
      def modify(f: S => S): FreeS.Par[F, Unit]
      def inspect[A](f: S => A): FreeS.Par[F, A]
    }

    object implicits {

      implicit def freestyleStateMHandler[M[_]](
          implicit MS: MonadState[M, S]): StateM.Handler[M] = new StateM.Handler[M] {
        def get: M[S]                   = MS.get
        def set(s: S): M[Unit]          = MS.set(s)
        def modify(f: S => S): M[Unit]  = MS.modify(f)
        def inspect[A](f: S => A): M[A] = MS.inspect(f)
      }

      class StateInspectFreeSLift[F[_]: StateM] extends FreeSLift[F, Function1[S, ?]] {
        def liftFSPar[A](fa: S => A): FreeS.Par[F, A] = StateM[F].inspect(fa)
      }

      implicit def freeSLiftStateInspect[F[_]: StateM]: FreeSLift[F, Function1[S, ?]] =
        new StateInspectFreeSLift[F]

    }

  }

  def apply[S] = new StateSeedProvider[S]

}
