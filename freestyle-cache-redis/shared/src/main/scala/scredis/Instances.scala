package freestyle.cache.redis.scredis

import cats.{~>}
import cats.data.Kleisli
import _root_.scredis.{Client ⇒ ScredisClient}

//
// The huge problem: How do I make sure that
// - Parallel Operations (in the FreeApplicative) are all joined into a single Kleisli
// - This single Kleisli is the only thing that goes into the `.withTransaction`.
//
class ScredisOpsInterpret[F[+ _]](client: ScredisClient) extends (ScredisOps[F, ?] ~> F) {

  override def apply[A](fa: Kleisli[F, ScredisCommands, A]): F[A] =
    client.withTransaction[F[A]](build ⇒ fa(build))

}
