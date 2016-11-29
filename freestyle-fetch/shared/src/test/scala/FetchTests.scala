package io.freestyle

import org.scalatest._
import _root_.fetch._
import _root_.fetch.implicits._
import cats.{Eval, Applicative}

import io.freestyle.fetch._
import io.freestyle.implicits._
import io.freestyle.fetch.implicits._
import scala.concurrent._
import scala.concurrent.duration._

class FetchTests extends AsyncWordSpec with Matchers {

  import algebras._
  import fetches._

  implicit override def executionContext = ExecutionContext.Implicits.global

   "Fetch Freestyle integration" should {

     "allow a fetch to be interleaved inside a program monadic flow" in {
       val program = for {
         a <- app.nonFetch.x
         b <- app.fetchM.runA(fetchString(a)).freeS
         c <- Applicative[FreeS[App.T, ?]].pure(1)
       } yield a + b + c
       program.exec[Future] map { _ shouldBe "111" }
     }

    "allow fetch syntax to lift to FreeS" in {
      val program: FreeS[App.T, String] = for {
        a <- app.nonFetch.x
        b <- fetchString(a).liftFS[App.T]
        c <- app.nonFetch.x
      } yield a + b + c
      program.exec[Future] map { _ shouldBe "111" }
    }

    "allow fetch syntax to lift to FreeS.Par" in { 
      val program: FreeS[App.T, String] = for {
        a <- app.nonFetch.x
        b <- fetchString(a).liftFSPar[App.T].freeS
        c <- app.nonFetch.x
      } yield a + b + c
      program.exec[Future] map { _ shouldBe "111" }
    }
 
  }

}

object algebras {
  @free trait NonFetch[F[_]] {
    def x: FreeS[F, Int]
  }

  implicit def nonFetchInterpreter: NonFetch.Interpreter[Future] = new NonFetch.Interpreter[Future] {
    def xImpl: Future[Int] = Future.successful(1)
  }

  @module trait App[F[_]] {
    val nonFetch: NonFetch[F]
    val fetchM: FetchM[F]
  }

  val app = App[App.T]

}

object datasources {
  import cats.data.NonEmptyList
  import cats.instances.list._

  implicit object ToStringSource extends DataSource[Int, String]{
    override def fetchOne(id: Int): Query[Option[String]] = {
      Query.sync(Option(id.toString))
    }
    override def fetchMany(ids: NonEmptyList[Int]): Query[Map[Int, String]] = {
      Query.sync(ids.toList.map(i => (i, i.toString)).toMap)
    }
  }

}

object fetches {
  import datasources._
  def fetchString(n: Int): Fetch[String] = Fetch(n) // or, more explicitly: Fetch(n)(ToStringSource)
}
