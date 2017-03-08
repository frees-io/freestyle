package freestyle

import cats.{~>, Foldable}
import _root_.doobie.imports._
import fs2.util.{Catchable, Suspendable}

object doobie {

  @free sealed trait DoobieM[F[_]] {
    def transact[A](f: ConnectionIO[A]): FreeS.Par[F, A]
  }

  object implicits {
    implicit def freeStyleDoobieHandler[M[_]: Catchable: Suspendable](
        implicit xa: Transactor[M]): DoobieM.Handler[M] =
      new DoobieM.Handler[M] {
        def transact[A](fa: ConnectionIO[A]): M[A] = fa.transact(xa)
      }

    implicit def freeSLiftDoobie[F[_]: DoobieM]: FreeSLift[F, ConnectionIO] =
      new FreeSLift[F, ConnectionIO] {
        def liftFSPar[A](cio: ConnectionIO[A]): FreeS.Par[F, A] = DoobieM[F].transact(cio)
      }
  }

}
