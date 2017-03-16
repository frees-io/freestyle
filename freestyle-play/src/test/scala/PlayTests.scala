package freestyle.http

import org.scalatest._

import scala.concurrent._
import scala.concurrent.duration._

import cats._

import freestyle._
import freestyle.implicits._

import freestyle.http.play._
import freestyle.http.play.implicits._

import _root_.play.api.mvc._
import _root_.play.api.http._

class PlayTests extends AsyncWordSpec with Matchers {
  import app._
  import cats.instances.future._

  implicit override def executionContext = ExecutionContext.Implicits.global

  def program[F[_] : Noop]: FreeS[F, Result] = for {
    x <- Noop[F].noop
  } yield Results.Ok(x)


  implicit def unitWr(implicit C: Codec): Writeable[Unit] = {
    Writeable(data => C.encode(data.toString))
  }

  implicit val unitCT: ContentTypeOf[Unit] = new ContentTypeOf(Option("text/plain"))

  "Play integration" should {
    "FreeSAction creates an action" in {
      FreeSAction { program[Noop.Op] }.isInstanceOf[Action[Result]] shouldBe true
    }
  }
}


object app {
  @free
  trait Noop[F[_]] {
    def noop: FreeS[F, Unit]
  }

  implicit def noopHandler[M[_]](
    implicit
      MM: Monad[M]
  ): Noop.Handler[M] = new Noop.Handler[M] {
    def noop: M[Unit] = MM.pure(())
  }
}
