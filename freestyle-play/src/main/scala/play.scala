package freestyle.http

import freestyle._

import cats.Monad

import scala.concurrent._

import _root_.play.api.mvc._
import _root_.play.api.http._


object play {
  object FreeSAction {
    def apply[A, F[_]](prog: FreeS[F, A])(
      implicit
        MF: Monad[Future],
        WA: Writeable[A],
        I: ParInterpreter[F, Future],
        EC: ExecutionContext
    ): Action[AnyContent] = {
      Action.async {
        prog.exec[Future].map (Results.Ok(_) : Result)
      }
    }
  }

  object implicits {

  }
}
