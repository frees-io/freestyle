package freestyle.redis.fscredis

import cats.{~>, Applicative}
import cats.data.Kleisli
import scredis.{Client ⇒ ScredisClient}

// This is just cats.data.KleisliApplicative[F[_], ScredisCommands]
// class ScredisOpsApplicative[F[+ _]](appF: Applicative[F]) extends Applicative[ScredisOps[F, ?]] {
class KleisliApplyOn[F[+ _], A](input: A) extends (Kleisli[F, A, ?] ~> F) {
  override def apply[B](karr: Kleisli[F, A, B]): F[B] = karr(input)
}

//
// The huge problem: How do I make sure that
// - Parallel Operations (in the FreeApplicative) are all joined into a single Kleisli
// - This single Kleisli is the only thing that goes into the `.withTransaction`.
//
class ScredisOpsInterpret[F[+ _]](client: ScredisClient) extends (ScredisOps[F, ?] ~> F) {

  override def apply[A](fa: Kleisli[F, ScredisCommands, A]): F[A] =
    client.withTransaction[F[A]](build ⇒ fa(build))

}
