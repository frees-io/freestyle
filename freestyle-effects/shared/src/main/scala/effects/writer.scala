package io.freestyle.effects

import io.freestyle._
import cats.MonadWriter

object writer {

  final class AccumulatorProvider[W] {

    @free sealed abstract class WriterM[F[_]] {
      def writer[A](aw: (W, A)): FreeS.Par[F, A]
      def tell(w: W): FreeS.Par[F,Unit] = writer((w, ()))
    }

    object implicits {

      implicit def freestyleWriterMInterpreter[M[_]](implicit MW: MonadWriter[M, W]): WriterM.Interpreter[M] = new WriterM.Interpreter[M] {
        def writerImpl[A](aw: (W, A)): M[A] = MW.writer(aw)
        def tell(w: W): M[Unit] = MW.tell(w)
      }

    }
 
  }

  def apply[W] = new AccumulatorProvider[W]

}
