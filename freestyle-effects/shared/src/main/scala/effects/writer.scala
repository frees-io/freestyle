package freestyle
package effects

import cats.MonadWriter

object writer {

  final class AccumulatorProvider[W] {

    @free sealed abstract class WriterM[F[_]] {
      def writer[A](aw: (W, A)): FreeS.Par[F, A]
      def tell(w: W): FreeS.Par[F, Unit]
    }

    object implicits {

      implicit def freestyleWriterMHandler[M[_]](
          implicit MW: MonadWriter[M, W]): WriterM.Handler[M] = new WriterM.Handler[M] {
        def writer[A](aw: (W, A)): M[A] = MW.writer(aw)
        def tell(w: W): M[Unit]         = MW.tell(w)
      }

    }

  }

  def apply[W] = new AccumulatorProvider[W]

}
