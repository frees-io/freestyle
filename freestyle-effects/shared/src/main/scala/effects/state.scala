package io.freestyle.effects

import io.freestyle._
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

      implicit def freestyleStateMInterpreter[M[_]](
          implicit MS: MonadState[M, S]): StateM.Interpreter[M] = new StateM.Interpreter[M] {
        def getImpl: M[S]                   = MS.get
        def setImpl(s: S): M[Unit]          = MS.set(s)
        def modifyImpl(f: S => S): M[Unit]  = MS.modify(f)
        def inspectImpl[A](f: S => A): M[A] = MS.inspect(f)
      }

    }

  }

  def apply[S] = new StateSeedProvider[S]

}
