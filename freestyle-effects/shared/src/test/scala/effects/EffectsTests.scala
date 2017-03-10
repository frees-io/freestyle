package freestyle.effects

import cats.{Applicative, Eval}

import org.scalatest._

import freestyle._
import freestyle.implicits._

import scala.concurrent._
import scala.concurrent.duration._

class EffectsTests extends AsyncWordSpec with Matchers {

  import collision._

  implicit override def executionContext = ExecutionContext.Implicits.global

  "Option Freestyle integration" should {

    import freestyle.effects.option._
    import freestyle.effects.option.implicits._

    "allow an Option to be interleaved inside a program monadic flow" in {
      import cats.implicits._
      def program[F[_]: OptionM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- OptionM[F].option(Option(1))
          c <- Applicative[FreeS[F, ?]].pure(1)
        } yield a + b + c
      program[OptionM.Op].exec[Option] shouldBe Some(3)
    }

    "allow an Option to shortcircuit inside a program monadic flow" in {
      import cats.implicits._
      def program[F[_]: OptionM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- OptionM[F].none[Int]
          c <- Applicative[FreeS[F, ?]].pure(1)
        } yield a + b + c
      program[OptionM.Op].exec[Option] shouldBe None
    }
  }

  "Error Freestyle integration" should {

    val ex = new RuntimeException("BOOM")

    import freestyle.effects.error._
    import freestyle.effects.error.implicits._

    "allow an Error to be interleaved inside a program monadic flow" in {
      import cats.implicits._
      def program[F[_]: ErrorM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- ErrorM[F].error[Int](ex)
          c <- Applicative[FreeS[F, ?]].pure(1)
        } yield a + b + c
      program[ErrorM.Op].exec[Either[Throwable, ?]] shouldBe Left(ex)
    }

    "allow an Exception to be captured inside a program monadic flow" in {
      import cats.implicits._
      def program[F[_]: ErrorM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- ErrorM[F].catchNonFatal[Int](Eval.later(throw ex))
          c <- Applicative[FreeS[F, ?]].pure(1)
        } yield a + b + c
      program[ErrorM.Op].exec[Either[Throwable, ?]] shouldBe Left(ex)
    }

    "allow an Either to propagate right biased" in {
      import cats.implicits._
      def program[F[_]: ErrorM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- ErrorM[F].either[Int](Right(1))
          c <- Applicative[FreeS[F, ?]].pure(1)
        } yield a + b + c
      program[ErrorM.Op].exec[Either[Throwable, ?]] shouldBe Right(3)
    }

    "allow an Either to short circuit" in {
      import cats.implicits._
      def program[F[_]: ErrorM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- ErrorM[F].either[Int](Left(ex))
          c <- Applicative[FreeS[F, ?]].pure(1)
        } yield a + b + c
      program[ErrorM.Op].exec[Either[Throwable, ?]] shouldBe Left(ex)
    }

  }

  "Reader integration" should {

    import freestyle.effects._
    import cats.data.Reader

    import rd.implicits._

    "allow retrieving an environment for a user defined type" in {
      import cats.implicits._
      def program[F[_]: rd.ReaderM] =
        for {
          _ <- Applicative[FreeS[F, ?]].pure(1)
          c <- rd.ReaderM[F].ask
          _ <- Applicative[FreeS[F, ?]].pure(1)
        } yield c
      program[rd.ReaderM.Op].exec[Reader[Config, ?]].run(Config()) shouldBe Config()
    }

    "allow maping over the environment for a user defined type" in {
      import cats.implicits._
      def program[F[_]: rd.ReaderM] =
        for {
          _ <- Applicative[FreeS[F, ?]].pure(1)
          c <- rd.ReaderM[F].reader(_.n)
          _ <- Applicative[FreeS[F, ?]].pure(1)
        } yield c
      program[rd.ReaderM.Op].exec[Reader[Config, ?]].run(Config()) shouldBe 5
    }

  }

  "State integration" should {

    import freestyle.effects._
    import cats.data.State

    import st.implicits._

    "get" in {
      import cats.implicits._
      def program[F[_]: st.StateM] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(1)
          b <- st.StateM[F].get
          c <- Applicative[FreeS[F, ?]].pure(1)
        } yield a + b + c
      program[st.StateM.Op].exec[State[Int, ?]].run(1).value shouldBe Tuple2(1, 3)
    }

    "set" in {
      import cats.implicits._
      def program[F[_]: st.StateM] =
        for {
          _ <- st.StateM[F].set(1)
          a <- st.StateM[F].get
        } yield a
      program[st.StateM.Op].exec[State[Int, ?]].run(0).value shouldBe Tuple2(1, 1)
    }

    "modify" in {
      import cats.implicits._
      def program[F[_]: st.StateM] =
        for {
          a <- st.StateM[F].get
          _ <- st.StateM[F].modify(_ + a)
          b <- st.StateM[F].get
        } yield b
      program[st.StateM.Op].exec[State[Int, ?]].run(1).value shouldBe Tuple2(2, 2)
    }

    "inspect" in {
      import cats.implicits._
      def program[F[_]: st.StateM] =
        for {
          a <- st.StateM[F].get
          b <- st.StateM[F].inspect(_ + a)
        } yield b
      program[st.StateM.Op].exec[State[Int, ?]].run(1).value shouldBe Tuple2(1, 2)
    }

    "syntax" in {
      import cats.implicits._
      def program[F[_]: st.StateM] =
        for {
          a <- st.StateM[F].get
          b <- ((x: Int) => x + a).liftFS
        } yield b
      program[st.StateM.Op].exec[State[Int, ?]].run(1).value shouldBe Tuple2(1, 2)
    }

  }

  "Writer integration" should {

    import freestyle.effects._
    import cats.data.Writer

    import wr.implicits._

    type Logger[A] = Writer[List[Int], A]

    "writer" in {
      import cats.implicits._
      def program[F[_]: wr.WriterM] =
        for {
          _ <- Applicative[FreeS[F, ?]].pure(1)
          b <- wr.WriterM[F].writer((Nil, 1))
          _ <- Applicative[FreeS[F, ?]].pure(1)
        } yield b
      program[wr.WriterM.Op].exec[Logger].run shouldBe Tuple2(Nil, 1)
    }

    "tell" in {
      import cats.implicits._
      def program[F[_]: wr.WriterM] =
        for {
          _ <- Applicative[FreeS[F, ?]].pure(1)
          b <- wr.WriterM[F].writer((List(1), 1))
          c <- wr.WriterM[F].tell(List(1))
          _ <- Applicative[FreeS[F, ?]].pure(1)
        } yield b
      program[wr.WriterM.Op].exec[Logger].run shouldBe Tuple2(List(1, 1), 1)
    }
  }

  "Validation integration" should {
    import freestyle.effects._

    import cats.data.{State, StateT}
    import cats.instances.future._
    import cats.instances.list._

    // Custom error types

    sealed trait ValidationException{
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
          _ <- Applicative[FreeS[F, ?]].pure(1)
          b <- vl.ValidationM[F].valid(42)
          _ <- Applicative[FreeS[F, ?]].pure(1)
        } yield b

      program[vl.ValidationM.Op].exec[Logger].runEmpty map {  _ shouldBe Tuple2(List(), 42) }
    }

    "invalid" in {
      def program[F[_]: vl.ValidationM] =
        for {
          _ <- Applicative[FreeS[F, ?]].pure(1)
          b <- vl.ValidationM[F].valid(42)
          _ <- vl.ValidationM[F].invalid(NotValid("oh"))
          _ <- vl.ValidationM[F].invalid(MissingFirstName)
          _ <- Applicative[FreeS[F, ?]].pure(1)
        } yield b

      val errors = List(NotValid("oh"), MissingFirstName)
      program[vl.ValidationM.Op].exec[Logger].runEmpty map { _  shouldBe Tuple2(errors, 42) }
    }

    "errors" in {
      val expectedErrors = List(NotValid("oh"), NotValid("no"))

      def program[F[_]: vl.ValidationM] =
        for {
          b <- vl.ValidationM[F].valid(42)
          _ <- vl.ValidationM[F].invalid(NotValid("oh"))
          _ <- vl.ValidationM[F].invalid(NotValid("no"))
          _ <- Applicative[FreeS[F, ?]].pure(1)
          actualErrors <- vl.ValidationM[F].errors
        } yield actualErrors == expectedErrors

      program[vl.ValidationM.Op].exec[Logger].runEmpty map { _  shouldBe Tuple2(expectedErrors, true) }
    }

    "fromEither" in {
      val expectedErrors = List(MissingFirstName)

      def program[F[_]: vl.ValidationM] =
        for {
          a <- vl.ValidationM[F].fromEither(Right(42))
          b <- vl.ValidationM[F].fromEither(Left(MissingFirstName): Either[ValidationException, Unit])
        } yield a

      program[vl.ValidationM.Op].exec[Logger].runEmpty.map { _ shouldBe Tuple2(expectedErrors, Right(42)) }
    }

    "fromValidatedNel" in {
      import cats.data.{Validated, ValidatedNel, NonEmptyList}

      def program[F[_]: vl.ValidationM] =
        for {
          a <- vl.ValidationM[F].fromValidatedNel(Validated.Valid(42))
          b <- vl.ValidationM[F].fromValidatedNel(
            Validated.invalidNel[ValidationException, Unit](MissingFirstName)
          )
        } yield a

      program[vl.ValidationM.Op].exec[Logger].runEmpty.map { _ shouldBe Tuple2(List(MissingFirstName), Validated.Valid(42)) }
    }

    "syntax" in {
      import cats.data.{Validated, ValidatedNel, NonEmptyList}

      def program[F[_]: vl.ValidationM] =
        for {
          a <- 42.valid
          b <- MissingFirstName.invalid
          c <- NotValid("no").invalid
        } yield a

      val expectedErrors = List(MissingFirstName, NotValid("no"))
      program[vl.ValidationM.Op].exec[Logger].runEmpty.map { _ shouldBe Tuple2(expectedErrors, 42) }
    }
  }

  "Traverse integration" should {

    import freestyle.effects._

    val list = traverse.list
    import list._, list.implicits._

    "fromTraversable" in {
      import cats.implicits._

      def program[F[_]: TraverseM] =
        for {
          a <- TraverseM[F].fromTraversable(1 :: 2 :: 3 :: Nil)
          b <- Applicative[FreeS[F, ?]].pure(a + 1)
        } yield b
      program[TraverseM.Op].exec[List] shouldBe List(2, 3, 4)
    }

    "empty" in {
      import cats.implicits._

      def program[F[_]: TraverseM] =
        for {
          _ <- TraverseM[F].empty[Int]
          a <- TraverseM[F].fromTraversable(1 :: 2 :: 3 :: Nil)
          b <- Applicative[FreeS[F, ?]].pure(a + 1)
          c <- Applicative[FreeS[F, ?]].pure(b + 1)
        } yield c
      program[TraverseM.Op].exec[List] shouldBe Nil
    }

    "syntax" in {
      import cats.implicits._

      def program[F[_]: TraverseM] =
        for {
          a <- (1 :: 2 :: 3 :: Nil).liftFS
          b <- Applicative[FreeS[F, ?]].pure(a + 1)
          c <- Applicative[FreeS[F, ?]].pure(b + 1)
        } yield c
      program[TraverseM.Op].exec[List] shouldBe List(3, 4, 5)
    }

  }

}

object collision {

  val wr = writer[List[Int]]

  val st = state[Int]

  case class Config(n: Int = 5)

  val rd = reader[Config]

  @module
  trait AppX[F[_]] {
    val stateM: st.StateM[F]
    val readerM: rd.ReaderM[F]
  }

  @free
  trait B[F[_]] {
    def x: FreeS[F, Int]
  }

  @free
  trait C[F[_]] {
    def x: FreeS[F, Int]
  }

  @free
  trait D[F[_]] {
    def x: FreeS[F, Int]
  }

  @free
  trait E[F[_]] {
    def x: FreeS[F, Int]
  }

  @module
  trait X[F[_]] {
    val a: B[F]
    val b: C[F]
  }

  @module
  trait Y[F[_]] {
    val c: C[F]
    val d: D[F]
  }

  @module
  trait Z[F[_]] {
    val x: X[F]
    val y: Y[F]
  }

}
