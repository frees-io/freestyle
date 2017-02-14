package freestyle.cache.redis.rediscala

import cats.{~>}
import cats.data.Kleisli
import _root_.redis.commands.Transactions

//
// The huge problem: How do I make sure that
// - Parallel Operations (in the FreeApplicative) are all joined into a single Kleisli
// - This single Kleisli is the only thing that goes into the `.withTransaction`.
//
class Interpret[F[+ _]](client: Transactions) extends (Ops[F, ?] ~> F) {

  override def apply[A](fa: Kleisli[F, Commands, A]): F[A] = {
    val transaction = client.transaction()
    val result      = fa(transaction)
    transaction.exec()
    result
  }

}
