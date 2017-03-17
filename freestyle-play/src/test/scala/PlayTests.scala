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
import _root_.play.api.test._

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

class PlayTests extends AsyncWordSpec with Matchers {
  implicit override def executionContext = ExecutionContext.Implicits.global

  // play

  implicit def unitWr(implicit C: Codec): Writeable[Unit] = {
    Writeable(data => C.encode(data.toString))
  }

  implicit val unitCT: ContentTypeOf[Unit] = new ContentTypeOf(Option("text/plain"))

  // akka

  implicit val actorSys: ActorSystem = ActorSystem("test")
  implicit val materializer: Materializer = ActorMaterializer()

  "Play integration" should {
    import cats.instances.future._

    import algebras._
    import handlers._

    def program[F[_] : Noop]: FreeS[F, Result] = for {
      x <- Noop[F].noop
    } yield Results.Ok(x)

    "FreeSAction creates an action" in {
      FreeSAction { program[Noop.Op] }.isInstanceOf[Action[Result]] shouldBe true
    }

    "The action writes the result in the response" in {
      import Helpers._

      val action: EssentialAction = FreeSAction { program[Noop.Op] }
      val request = FakeRequest()
      val response: Future[Result] = Helpers.call(action, request)

      status(response) shouldBe 200
      contentAsString(response) shouldBe "()"
    }
  }
}

object algebras {
  @free
  trait Noop[F[_]] {
    def noop: FreeS[F, Unit]
  }
}

object handlers {
  import algebras._

  implicit def noopHandler[M[_]](
    implicit
      MM: Monad[M]
  ): Noop.Handler[M] = new Noop.Handler[M] {
    def noop: M[Unit] = MM.pure(())
  }
}
