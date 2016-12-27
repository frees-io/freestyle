package io.freestyle

import cats.Applicative
import cats.syntax.either._
import fs2.Task
import fs2.interop.cats._
import org.scalatest._
import _root_.doobie.imports._
import _root_.doobie.h2.h2transactor._

import io.freestyle.implicits._
import io.freestyle.doobie._
import io.freestyle.doobie.implicits._

import scala.language.postfixOps

class DoobieTests extends AsyncWordSpec with Matchers {

  import algebras._

  implicit val xa: Transactor[Task] =
    H2Transactor[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "").unsafeRunSync.toOption
      .getOrElse(throw new Exception("Could not create test transactor"))

  val query: ConnectionIO[Int] = sql"SELECT 1 + 1".query[Int].unique

  "Doobie Freestyle integration" should {

    "allow a doobie ConnectionIO program to be interleaved inside a program monadic flow" in {
      val program = for {
        a <- app.nonDoobie.x
        b <- app.doobieM.transact(query).freeS
        c <- Applicative[FreeS[App.T, ?]].pure(1)
      } yield a + b + c
      program.exec[Task] map { _ shouldBe 4 } unsafeRunAsyncFuture
    }

    "allow doobie syntax to lift to FreeS" in {
      val program: FreeS[App.T, Int] = for {
        a <- app.nonDoobie.x
        b <- query.liftFS[App.T]
        c <- app.nonDoobie.x
      } yield a + b + c
      program.exec[Task] map { _ shouldBe 4 } unsafeRunAsyncFuture
    }

    "allow doobie syntax to lift to FreeS.Par" in {
      val program: FreeS[App.T, Int] = for {
        a <- app.nonDoobie.x
        b <- query.liftFSPar[App.T].freeS
        c <- app.nonDoobie.x
      } yield a + b + c
      program.exec[Task] map { _ shouldBe 4 } unsafeRunAsyncFuture
    }
  }

}

object algebras {
  @free
  trait NonDoobie[F[_]] {
    def x: FreeS[F, Int]
  }

  implicit def nonDoobieInterpreter: NonDoobie.Interpreter[Task] =
    new NonDoobie.Interpreter[Task] {
      def xImpl: Task[Int] = Task.now(1)
    }

  @module
  trait App[F[_]] {
    val nonDoobie: NonDoobie[F]
    val doobieM: DoobieM[F]
  }

  val app = App[App.T]
}
