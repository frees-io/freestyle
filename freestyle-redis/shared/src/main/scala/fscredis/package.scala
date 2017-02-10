package freestyle.redis

import scredis.commands.{
  KeyCommands,
  ListCommands,
  ScriptingCommands,
  ServerCommands,
  SetCommands,
  StringCommands
}
import scredis.serialization.{Reader, Writer}

package object fscredis {

  type ScredisCommands =
    KeyCommands with ListCommands with ScriptingCommands with ServerCommands with SetCommands with StringCommands

  type ScredisOps[F[+ _], +A] = ScredisCommands ⇒ F[A]

}
