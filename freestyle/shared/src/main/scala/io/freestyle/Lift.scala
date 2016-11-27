package io.freestyle

import cats.{Applicative, Id}
import cats.free.{Inject, FreeApplicative}

trait FreeSLift[F[_], G[_]] {
  def liftFS[A](ga: G[A]): FreeS[F, A] = liftFSPar[A](ga).freeS
  def liftFSPar[A](ga: G[A]): FreeS.Par[F, A]
}




