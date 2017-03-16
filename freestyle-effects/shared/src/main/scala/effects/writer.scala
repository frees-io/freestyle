package freestyle
package effects

import cats.MonadWriter

object writer {

  final class AccumulatorProvider[W] {

    @free sealed abstract class WriterM {
      def writer[A](aw: (W, A)): Oper.Par[A]
      def tell(w: W): Oper.Par[Unit]
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
