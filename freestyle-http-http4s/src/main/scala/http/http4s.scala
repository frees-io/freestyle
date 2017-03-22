package freestyle
package http

import cats.{~>, Monad}
import org.http4s.EntityEncoder

import freestyle.implicits._

object http4s {

  implicit def freeSEntityEncoder[F[_], G[_], A](
      implicit NT: F ~> G,
      G: Monad[G],
      EE: EntityEncoder[G[A]]): EntityEncoder[FreeS[F, A]] =
    EE.contramap((f: FreeS[F, A]) => f.exec[G])

}
