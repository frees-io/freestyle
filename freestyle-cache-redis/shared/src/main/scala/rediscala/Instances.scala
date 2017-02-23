package freestyle.cache.redis.rediscala

import cats.{~>}
import cats.data.Kleisli
import _root_.redis.commands.Transactions

/* An important challenge when executing operations in FreeStyle is to ensure that
 * parallel fragments execute their operations as _parallel_ as possible.
 * Since Redis is a single-threaded server, _parallel_ means sending operations together
 * in a single batch, which is possible if there are no data dependencies between them. */
class Interpret[F[_]](client: Transactions) extends (Ops[F, ?] ~> F) {

  override def apply[A](fa: Kleisli[F, Commands, A]): F[A] = {
    val transaction = client.transaction()
    val result      = fa(transaction)
    transaction.exec()
    result
  }

}
