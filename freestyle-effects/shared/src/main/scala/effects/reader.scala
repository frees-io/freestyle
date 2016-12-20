package io.freestyle.effects

import io.freestyle._
import cats.{Monad, MonadReader, Eval}
import cats.arrow.FunctionK

object reader {

  final class EnvironmentProvider[R] {

    @free sealed abstract class ReaderM[F[_]] {
      def ask: FreeS.Par[F, R]
      def reader[B](f: R => B): FreeS.Par[F, B]
    }

    object implicits {

      implicit def interpreter[M[_]](implicit MR: MonadReader[M, R]): ReaderM.Interpreter[M] = new ReaderM.Interpreter[M] {
        def askImpl: M[R] = MR.ask
        def readerImpl[B](f: R => B): M[B] = MR.reader(f)
      }

    }

  }

  def apply[R] = new EnvironmentProvider[R]

}
