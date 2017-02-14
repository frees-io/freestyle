package freestyle.cache.redis

import cats.data.Kleisli
import _root_.redis.commands.{Keys, Server, Strings}

package object rediscala {

  type Commands = Keys with Server with Strings

  type Ops[F[+ _], A] = Kleisli[F, Commands, A]

}
