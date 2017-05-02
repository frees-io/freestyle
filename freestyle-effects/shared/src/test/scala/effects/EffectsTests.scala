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

package freestyle.effects

import cats.{Applicative, Eval}

import org.scalatest._

import freestyle._
import freestyle.implicits._

import scala.concurrent.{ExecutionContext, Future}

class EffectsTests extends AsyncWordSpec with Matchers {

  import collision._

  implicit override def executionContext = ExecutionContext.Implicits.global

  "Option Freestyle integration" should {

    import freestyle.effects.option._
    import freestyle.effects.option.implicits._

    import cats.instances.option._

    "allow an Option to be interleaved inside a program monadic flow" in {
      def program[F[_]: OptionM] =
        for {
          a <- FreeS.pure(1)
          b <- OptionM[F].option(Option(1))
          c <- FreeS.pure(1)
        } yield a + b + c
      program[OptionM.Op].interpret[Option] shouldBe Some(3)
    }

    "allow an Option to shortcircuit inside a program monadic flow" in {
      def program[F[_]: OptionM] =
        for {
          a <- FreeS.pure(1)
          b <- OptionM[F].none[Int]
          c <- FreeS.pure(1)
        } yield a + b + c
      program[OptionM.Op].interpret[Option] shouldBe None
    }

    "allow an Option to be interleaved inside a program monadic flow using syntax" in {
      def program[F[_]: OptionM] =
        for {
          a <- FreeS.pure(1)
          b <- Option(1).liftFS
          c <- FreeS.pure(1)
        } yield a + b + c
      program[OptionM.Op].interpret[Option] shouldBe Some(3)
    }
  }

  "Error Freestyle integration" should {

    val ex = new RuntimeException("BOOM")

    import freestyle.effects.error._
    import freestyle.effects.error.implicits._

    import cats.instances.either._
    import cats.syntax.either._

    "allow an Error to be interleaved inside a program monadic flow" in {
      def program[F[_]: ErrorM] =
        for {
          a <- FreeS.pure(1)
          b <- ErrorM[F].error[Int](ex)
          c <- FreeS.pure(1)
        } yield a + b + c
      program[ErrorM.Op].interpret[Either[Throwable, ?]] shouldBe Left(ex)
    }

    "allow an Exception to be captured inside a program monadic flow" in {
      def program[F[_]: ErrorM] =
        for {
          a <- FreeS.pure(1)
          b <- ErrorM[F].catchNonFatal[Int](Eval.later(throw ex))
          c <- FreeS.pure(1)
        } yield a + b + c
      program[ErrorM.Op].interpret[Either[Throwable, ?]] shouldBe Left(ex)
    }

    "allow an Either to propagate right biased" in {
      def program[F[_]: ErrorM] =
        for {
          a <- FreeS.pure(1)
          b <- ErrorM[F].either[Int](Right(1))
          c <- FreeS.pure(1)
        } yield a + b + c
      program[ErrorM.Op].interpret[Either[Throwable, ?]] shouldBe Right(3)
    }

    "allow an Either to short circuit" in {
      def program[F[_]: ErrorM] =
        for {
          a <- FreeS.pure(1)
          b <- ErrorM[F].either[Int](Left(ex))
          c <- FreeS.pure(1)
        } yield a + b + c
      program[ErrorM.Op].interpret[Either[Throwable, ?]] shouldBe Left(ex)
    }

    "allow an Either to propagate right biased using syntax" in {
      def program[F[_]: ErrorM] =
        for {
          a <- FreeS.pure(1)
          b <- Either.right[Throwable, Int](1).liftFS
          c <- FreeS.pure(1)
        } yield a + b + c
      program[ErrorM.Op].interpret[Either[Throwable, ?]] shouldBe Right(3)
    }

    "allow an Either to short circuit using syntax" in {
      def program[F[_]: ErrorM] =
        for {
          a <- FreeS.pure(1)
          b <- Either.left[Throwable, Int](ex).liftFS
          c <- FreeS.pure(1)
        } yield a + b + c
      program[ErrorM.Op].interpret[Either[Throwable, ?]] shouldBe Left(ex)
    }

  }

  "Either Freestyle integration" should {

    sealed trait CustomError
    case object Custom1 extends CustomError

    import freestyle.effects.either

    val e = either[CustomError]
    val ex = Custom1

    import e.implicits._

    import cats.instances.either._
    import cats.syntax.either._

    "allow an Either to be interleaved inside a program monadic flow" in {
      def program[F[_]: e.EitherM] =
        for {
          a <- FreeS.pure(1)
          b <- e.EitherM[F].error[Int](ex)
          c <- FreeS.pure(1)
        } yield a + b + c
      program[e.EitherM.Op].interpret[Either[CustomError, ?]] shouldBe Left(ex)
    }

    "allow an Exception to be captured inside a program monadic flow" in {
      def program[F[_]: e.EitherM] =
        for {
          a <- FreeS.pure(1)
          b <- e.EitherM[F].catchNonFatal[Int](Eval.later(throw new RuntimeException("BOOM")), _ => ex)
          c <- FreeS.pure(1)
        } yield a + b + c
      program[e.EitherM.Op].interpret[Either[CustomError, ?]] shouldBe Left(ex)
    }

    "allow an Either to propagate right biased" in {
      def program[F[_]: e.EitherM] =
        for {
          a <- FreeS.pure(1)
          b <- e.EitherM[F].either[Int](Right(1))
          c <- FreeS.pure(1)
        } yield a + b + c
      program[e.EitherM.Op].interpret[Either[CustomError, ?]] shouldBe Right(3)
    }

    "allow an Either to short circuit" in {
      def program[F[_]: e.EitherM] =
        for {
          a <- FreeS.pure(1)
          b <- e.EitherM[F].either[Int](Left(ex))
          c <- FreeS.pure(1)
        } yield a + b + c
      program[e.EitherM.Op].interpret[Either[CustomError, ?]] shouldBe Left(ex)
    }

    "allow an Either to propagate right biased using syntax" in {
      def program[F[_]: e.EitherM] =
        for {
          a <- FreeS.pure(1)
          b <- Either.right[CustomError, Int](1).liftFS
          c <- FreeS.pure(1)
        } yield a + b + c
      program[e.EitherM.Op].interpret[Either[CustomError, ?]] shouldBe Right(3)
    }

    "allow an Either to short circuit using syntax" in {
      def program[F[_]: e.EitherM] =
        for {
          a <- FreeS.pure(1)
          b <- Either.left[CustomError, Int](ex).liftFS
          c <- FreeS.pure(1)
        } yield a + b + c
      program[e.EitherM.Op].interpret[Either[CustomError, ?]] shouldBe Left(ex)
    }

  }

  "Reader integration" should {

    import freestyle.effects._
    import cats.data.Reader

    import rd.implicits._

    "allow retrieving an environment for a user defined type" in {
      def program[F[_]: rd.ReaderM] =
        for {
          _ <- FreeS.pure(1)
          c <- rd.ReaderM[F].ask
          _ <- FreeS.pure(1)
        } yield c
      program[rd.ReaderM.Op].interpret[Reader[Config, ?]].run(Config()) shouldBe Config()
    }

    "allow maping over the environment for a user defined type" in {
      def program[F[_]: rd.ReaderM] =
        for {
          _ <- FreeS.pure(1)
          c <- rd.ReaderM[F].reader(_.n)
          _ <- FreeS.pure(1)
        } yield c
      program[rd.ReaderM.Op].interpret[Reader[Config, ?]].run(Config()) shouldBe 5
    }

  }

  "State integration" should {

    import freestyle.effects._
    import cats.data.State

    import st.implicits._

    "get" in {
      def program[F[_]: st.StateM] =
        for {
          a <- FreeS.pure(1)
          b <- st.StateM[F].get
          c <- FreeS.pure(1)
        } yield a + b + c
      program[st.StateM.Op].interpret[State[Int, ?]].run(1).value shouldBe Tuple2(1, 3)
    }

    "set" in {
      def program[F[_]: st.StateM] =
        for {
          _ <- st.StateM[F].set(1)
          a <- st.StateM[F].get
        } yield a
      program[st.StateM.Op].interpret[State[Int, ?]].run(0).value shouldBe Tuple2(1, 1)
    }

    "modify" in {
      def program[F[_]: st.StateM] =
        for {
          a <- st.StateM[F].get
          _ <- st.StateM[F].modify(_ + a)
          b <- st.StateM[F].get
        } yield b
      program[st.StateM.Op].interpret[State[Int, ?]].run(1).value shouldBe Tuple2(2, 2)
    }

    "inspect" in {
      def program[F[_]: st.StateM] =
        for {
          a <- st.StateM[F].get
          b <- st.StateM[F].inspect(_ + a)
        } yield b
      program[st.StateM.Op].interpret[State[Int, ?]].run(1).value shouldBe Tuple2(1, 2)
    }

    "syntax" in {
      def program[F[_]: st.StateM] =
        for {
          a <- st.StateM[F].get
          b <- ((x: Int) => x + a).liftFS
        } yield b
      program[st.StateM.Op].interpret[State[Int, ?]].run(1).value shouldBe Tuple2(1, 2)
    }

  }

  "Writer integration" should {

    import freestyle.effects._
    import cats.data.Writer
    import cats.instances.list._

    import wr.implicits._

    type Logger[A] = Writer[List[Int], A]

    "writer" in {
      def program[F[_]: wr.WriterM] =
        for {
          _ <- FreeS.pure(1)
          b <- wr.WriterM[F].writer((Nil, 1))
          _ <- FreeS.pure(1)
        } yield b
      program[wr.WriterM.Op].interpret[Logger].run shouldBe Tuple2(Nil, 1)
    }

    "tell" in {
      def program[F[_]: wr.WriterM] =
        for {
          _ <- FreeS.pure(1)
          b <- wr.WriterM[F].writer((List(1), 1))
          c <- wr.WriterM[F].tell(List(1))
          _ <- FreeS.pure(1)
        } yield b
      program[wr.WriterM.Op].interpret[Logger].run shouldBe Tuple2(List(1, 1), 1)
    }
  }

  "Validation integration" should {
    import freestyle.effects._

    import cats.data.{State, StateT}
    import cats.instances.future._
    import cats.instances.list._

    // Custom error types

    sealed trait ValidationException {
      def explanation: String
    }
    case class NotValid(explanation: String) extends ValidationException
    case object MissingFirstName extends ValidationException {
      val explanation = "The first name is missing"
    }

    type Errors = List[ValidationException]

    // Validation for custom errors

    val vl = validation[ValidationException]
    import vl.implicits._

    // Runtime

    type Logger[A] = StateT[Future, Errors, A]

    "valid" in {
      def program[F[_]: vl.ValidationM] =
        for {
          _ <- FreeS.pure(1)
          b <- vl.ValidationM[F].valid(42)
          _ <- FreeS.pure(1)
        } yield b

      program[vl.ValidationM.Op].interpret[Logger].runEmpty map { _ shouldBe Tuple2(List(), 42) }
    }

    "invalid" in {
      def program[F[_]: vl.ValidationM] =
        for {
          _ <- FreeS.pure(1)
          b <- vl.ValidationM[F].valid(42)
          _ <- vl.ValidationM[F].invalid(NotValid("oh"))
          _ <- vl.ValidationM[F].invalid(MissingFirstName)
          _ <- FreeS.pure(1)
        } yield b

      val errors = List(NotValid("oh"), MissingFirstName)
      program[vl.ValidationM.Op].interpret[Logger].runEmpty map { _ shouldBe Tuple2(errors, 42) }
    }

    "errors" in {
      val expectedErrors = List(NotValid("oh"), NotValid("no"))

      def program[F[_]: vl.ValidationM] =
        for {
          b            <- vl.ValidationM[F].valid(42)
          _            <- vl.ValidationM[F].invalid(NotValid("oh"))
          _            <- vl.ValidationM[F].invalid(NotValid("no"))
          _            <- FreeS.pure(1)
          actualErrors <- vl.ValidationM[F].errors
        } yield actualErrors == expectedErrors

      program[vl.ValidationM.Op].interpret[Logger].runEmpty map {
        _ shouldBe Tuple2(expectedErrors, true)
      }
    }

    "fromEither" in {
      import cats.syntax.either._

      val expectedErrors = List(MissingFirstName)

      def program[F[_]: vl.ValidationM] =
        for {
          a <- vl.ValidationM[F].fromEither(Right(42))
          b <- vl
            .ValidationM[F]
            .fromEither(Either.left[ValidationException, Unit](MissingFirstName))
        } yield a

      program[vl.ValidationM.Op].interpret[Logger].runEmpty.map {
        _ shouldBe Tuple2(expectedErrors, Right(42))
      }
    }

    "fromValidatedNel" in {
      import cats.data.Validated

      def program[F[_]: vl.ValidationM] =
        for {
          a <- vl.ValidationM[F].fromValidatedNel(Validated.valid(42))
          b <- vl
            .ValidationM[F]
            .fromValidatedNel(
              Validated.invalidNel[ValidationException, Unit](MissingFirstName)
            )
        } yield a

      program[vl.ValidationM.Op].interpret[Logger].runEmpty.map {
        _ shouldBe Tuple2(List(MissingFirstName), Validated.valid(42))
      }
    }

    "syntax" in {
      def program[F[_]: vl.ValidationM] =
        for {
          a <- 42.liftValid
          b <- MissingFirstName.liftInvalid
          c <- NotValid("no").liftInvalid
        } yield a

      val expectedErrors = List(MissingFirstName, NotValid("no"))
      program[vl.ValidationM.Op].interpret[Logger].runEmpty.map {
        _ shouldBe Tuple2(expectedErrors, 42)
      }
    }
  }

  "Traverse integration" should {

    import freestyle.effects._
    import cats.instances.list._

    val list = traverse.list
    import list._, list.implicits._

    "fromTraversable" in {
      def program[F[_]: TraverseM] =
        for {
          a <- TraverseM[F].fromTraversable(1 :: 2 :: 3 :: Nil)
          b <- FreeS.pure(a + 1)
        } yield b
      program[TraverseM.Op].interpret[List] shouldBe List(2, 3, 4)
    }

    "empty" in {
      def program[F[_]: TraverseM] =
        for {
          _ <- TraverseM[F].empty[Int]
          a <- TraverseM[F].fromTraversable(1 :: 2 :: 3 :: Nil)
          b <- FreeS.pure(a + 1)
          c <- FreeS.pure(b + 1)
        } yield c
      program[TraverseM.Op].interpret[List] shouldBe Nil
    }

    "syntax" in {
      def program[F[_]: TraverseM] =
        for {
          a <- (1 :: 2 :: 3 :: Nil).liftFS
          b <- FreeS.pure(a + 1)
          c <- FreeS.pure(b + 1)
        } yield c
      program[TraverseM.Op].interpret[List] shouldBe List(3, 4, 5)
    }

  }

  "Uber implicits import" should {
    import freestyle.effects.option._
    import freestyle.effects.error._
    import freestyle.effects.implicits._

    "import option implicits" in {
      import cats.instances.option._

      def program[F[_]: OptionM] =
        for {
          a <- FreeS.pure(1)
          b <- OptionM[F].none[Int]
          c <- FreeS.pure(1)
        } yield a + b + c

      program[OptionM.Op].interpret[Option] shouldBe None
    }

    "import error implicits" in {
      import cats.instances.either._

      val ex = new RuntimeException("BOOM")

      def program[F[_]: ErrorM] =
        for {
          a <- FreeS.pure(1)
          b <- ErrorM[F].error[Int](ex)
          c <- FreeS.pure(1)
        } yield a + b + c

      program[ErrorM.Op].interpret[Either[Throwable, ?]] shouldBe Left(ex)
    }
  }

}

object collision {

  val wr = writer[List[Int]]

  val st = state[Int]

  case class Config(n: Int = 5)

  val rd = reader[Config]

  @module
  trait AppX {
    val stateM: st.StateM
    val readerM: rd.ReaderM
  }

  @free
  trait B {
    def x: FS[Int]
  }

  @free
  trait C {
    def x: FS[Int]
  }

  @free
  trait D {
    def x: FS[Int]
  }

  @free
  trait E {
    def x: FS[Int]
  }

  @module
  trait X {
    val a: B
    val b: C
  }

  @module
  trait Y {
    val c: C
    val d: D
  }

  @module
  trait Z {
    val x: X
    val y: Y
  }

}
