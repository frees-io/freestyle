package io.freestyle.effects

import io.freestyle._

object rws {

  final class RWS[R, W, S] {

    val r = io.freestyle.effects.reader[R]
    val w = io.freestyle.effects.writer[W]
    val s = io.freestyle.effects.state[S]

    @module trait ReaderWriterState[F[_]] {
      val reader: r.ReaderM[F]
      val writer: w.WriterM[F]
      val state: s.StateM[F]
    }

  }

  def apply[R, W, S] = new RWS[R, W, S]

}
