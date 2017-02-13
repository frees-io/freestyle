package freestyle.cache.redis

import cats.{~>}
import cats.data.Kleisli
import scala.concurrent.Future

import _root_.scredis.commands.{
  KeyCommands,
  ListCommands,
  ScriptingCommands,
  ServerCommands,
  SetCommands,
  StringCommands
}
import _root_.scredis.serialization.{Reader, Writer}

package object scredis {

  type ScredisCommands =
    KeyCommands with ListCommands with ScriptingCommands with ServerCommands with SetCommands with StringCommands

  type ScredisOps[F[+ _], A] = Kleisli[F, ScredisCommands, A]

  type RawScredisOps[A] = ScredisOps[Future, A]

}
