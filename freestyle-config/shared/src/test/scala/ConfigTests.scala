package io.freestyle

import org.scalatest._

import io.freestyle.config._
import io.freestyle.implicits._
import io.freestyle.config.implicits._
import scala.concurrent._
import scala.concurrent.duration._
import cats.implicits._

class ConfigTests extends AsyncWordSpec with Matchers {

  import algebras._

  implicit override def executionContext = ExecutionContext.Implicits.global

  "Shocon config integration" should {

    "allow configuration to be interleaved inside a program monadic flow" in {
      val program = for {
        _      <- app.nonConfig.x
        config <- app.configM.empty
      } yield config
      program.exec[Future] map { _.isInstanceOf[Config] shouldBe true }
    }

    "allow configuration to parse strings" in {
      val program = app.configM.parseString("{n = 1}")
      program.exec[Future] map { _.int("n") shouldBe Some(1) }
    }

    "allow configuration to load classpath files" in {
      val program = app.configM.load
      program.exec[Future] map { _.int("s") shouldBe Some(3) }
    }

  }

}

object algebras {
  @free
  trait NonConfig[F[_]] {
    def x: FreeS[F, Int]
  }

  implicit def nonConfigInterpreter: NonConfig.Interpreter[Future] =
    new NonConfig.Interpreter[Future] {
      def xImpl: Future[Int] = Future.successful(1)
    }

  @module
  trait App[F[_]] {
    val nonConfig: NonConfig[F]
    val configM: ConfigM[F]
  }

  val app = App[App.T]

}
