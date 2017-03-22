package freestyle
package effects

import cats.MonadReader

object reader {

  final class EnvironmentProvider[R] {

    @free sealed abstract class ReaderM {
      def ask: OpPar[R]
      def reader[B](f: R => B): OpPar[B]
    }

    object implicits {

      implicit def freestyleReaderMHandler[M[_]](implicit MR: MonadReader[M, R]): ReaderM.Handler[M] =
        new ReaderM.Handler[M] {
          def ask: M[R]                  = MR.ask
          def reader[B](f: R => B): M[B] = MR.reader(f)
        }
    }

  }

  def apply[R] = new EnvironmentProvider[R]

}
