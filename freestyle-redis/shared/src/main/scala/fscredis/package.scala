package freestyle.redis

import cats.data.Kleisli
import scredis.commands.{KeyCommands, ServerCommands, StringCommands}

package object fscredis {

  type ScredisCommands = KeyCommands with ServerCommands with StringCommands

  type ScredisOps[F[+ _], A] = Kleisli[F, ScredisCommands, A]

}
