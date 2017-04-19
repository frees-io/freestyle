/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle

import org.scalatest._
import _root_.fs2._
import _root_.fs2.util.Free
import cats.Applicative

import fs2._

import freestyle.fs2._
import freestyle.implicits._
import freestyle.fs2.implicits._
import cats.Monoid
import cats.instances.future._
import scala.concurrent._
import scala.concurrent.duration._

class Fs2Tests extends AsyncWordSpec with Matchers {
  implicit override def executionContext = ExecutionContext.Implicits.global

  "Fetch Freestyle integration" should {
    import algebras._

    "allow a stream to be interleaved inside a program monadic flow" in {
      val program = for {
        a <- app.nonStream.x
        b <- app.streamM.runLog(Stream.emit(40))
        c <- Applicative[FreeS[App.Op, ?]].pure(1)
      } yield a + b.head + c
      program.exec[Future] map { _ shouldBe 42 }
    }

    "allow a stream to be folded inside a program monadic flow" in {
      val program = for {
        a <- app.nonStream.x
        b <- app.streamM.runFold(0, (x: Int, y: Int) => x + y)(Stream.emits(List(39, 1, 1)))
      } yield a + b
      program.exec[Future] map { _ shouldBe 42 }
    }

    "allow an empty stream to be consumed inside a program monadic flow" in {
      val program = for {
        a <- app.nonStream.x
        b <- app.streamM.runLast(Stream.emits(List.empty[Int]))
      } yield b

      program.exec[Future] map { _ shouldBe None }
    }

    "allow a stream to be consumed for its last element inside a program monadic flow" in {
      val program = for {
        a <- app.nonStream.x
        b <- app.streamM.runLast(Stream.emits(List(1, 2, 41)))
      } yield b.fold(0)(_ + a)

      program.exec[Future] map { _ shouldBe 42 }
    }

    "allow a stream to be run for its effects inside a program monadic flow" in {
      val program = for {
        a <- app.nonStream.x
        _ <- app.streamM.run(Stream.eval(Free.pure(42)))
      } yield a

      program.exec[Future] map { _ shouldBe 1 }
    }

    case class OhNoException() extends Exception

    "allow stream errors to be recovered from" in {
      val program = for {
        a <- app.nonStream.x
        b <- app.streamM.runFold(0, (x: Int, y: Int) => x + y)(Stream.fail(OhNoException()))
      } yield a + b

      program.exec[Future] recover {
        case OhNoException() => 42
      } map { _ shouldBe 42 }
    }

    implicit val sumMonoid: Monoid[Int] = new Monoid[Int] {
      def empty: Int                   = 0
      def combine(x: Int, y: Int): Int = x + y
    }

    "allow streams to be lifted to a FreeS program" in {
      val stream: Stream[Eff, Int] = Stream.emits(List(1, 1, 40))

      val program = for {
        v <- stream.liftFS[App.Op]
      } yield v

      program.exec[Future] map { _ shouldBe 42 }
    }

    "allow streams to be lifted to a FreeS parallel program" in {
      val stream: Stream[Eff, Int] = Stream.emits(List(1, 1, 40))

      val program = for {
        v <- stream.liftFSPar[App.Op]
      } yield v

      program.exec[Future] map { _ shouldBe 42 }
    }
  }

}

object algebras {
  @free
  trait NonStream {
    def x: OpSeq[Int]
  }

  implicit def nonStreamHandler: NonStream.Handler[Future] =
    new NonStream.Handler[Future] {
      def x: Future[Int] = Future.successful(1)
    }

  @module
  trait App[F[_]] {
    val nonStream: NonStream[F]
    val streamM: StreamM[F]
  }

  val app = App[App.Op]
}
