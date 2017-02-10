package freestyle.redis.fscredis

import cats.{~>, Applicative}
import scredis.{Client ⇒ ScredisClient}

class ScredisOpsApplicative[F[+ _]](appF: Applicative[F]) extends Applicative[ScredisOps[F, ?]] {

  override def pure[A](x: A): ScredisOps[F, A] =
    _client ⇒ appF.pure(x)

  override def ap[A, B](ff: ScredisOps[F, A ⇒ B])(fa: ScredisOps[F, A]): ScredisOps[F, B] =
    client ⇒ {
      val lhs = ff(client)
      val rhs = fa(client)
      appF.ap(lhs)(rhs)
    }

}

class ScredisOpsInterpret[F[+ _]](client: ScredisClient) extends (ScredisOps[F, ?] ~> F) {

  override def apply[A](fa: ScredisOps[F, A]): F[A] =
    client.withTransaction[F[A]](build ⇒ fa(build))

}

object implicits {

  implicit def applicativeScredisOps[F[+ _]](
      implicit appF: Applicative[F]): Applicative[ScredisOps[F, ?]] =
    new ScredisOpsApplicative[F](appF)

}
