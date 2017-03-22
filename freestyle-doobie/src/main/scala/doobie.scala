package freestyle

import cats.{~>, Foldable}
import _root_.doobie.imports.{ConnectionIO, Transactor}
import fs2.util.{Catchable, Suspendable}

object doobie {

  @free sealed trait DoobieM {
    def transact[A](f: ConnectionIO[A]): OpPar[A]
  }

  object implicits {
    implicit def freeStyleDoobieHandler[M[_]: Catchable: Suspendable](
        implicit xa: Transactor[M]): DoobieM.Handler[M] =
      new DoobieM.Handler[M] {
        def transact[A](fa: ConnectionIO[A]): M[A] = xa.trans(fa)
      }

    implicit def freeSLiftDoobie[F[_]](implicit TO: DoobieM.To[F]): FreeSLift[F, ConnectionIO] =
      new FreeSLift[F, ConnectionIO] {
        def liftFSPar[A](cio: ConnectionIO[A]): FreeS.Par[F, A] = TO.transact(cio)
      }
  }

}
