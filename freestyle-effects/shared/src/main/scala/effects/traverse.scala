package io.freestyle.effects

import io.freestyle._
import cats.{~>, MonadCombine, Traverse}
import cats.data.ValidatedNel
import cats.free.Free

import cats.implicits._

object traverse {

  final class TraverseProvider[G[_]] {

    /** Acts as a generator providing traversable semantics to programs
     */
    @free sealed abstract class TraverseM[F[_]] {
      def empty[A]: FreeS[F, A]
      def singleton[A](a: A): FreeS[F, A]
      def fromTraversable[A](ta: G[A]): FreeS[F, A]
    }

    /** Interpretable as long as traverse instance for G[_] and a monad combine for G[_] exists
     * in scope
     */
    object implicits {
      implicit def freestyleTraverseMInterpreter[F[_], M[_]](
          implicit MC: MonadCombine[M],
          FT: Traverse[G]): TraverseM.Interpreter[M] =
        new TraverseM.Interpreter[M] {
          def emptyImpl[A]: M[A]           = MC.empty[A]
          def singletonImpl[A](a: A): M[A] = MC.pure[A](a)
          def fromTraversableImpl[A](ta: G[A]): M[A] = {
            val els: M[G[A]] = FT.traverse(ta)(singletonImpl)
            MC.unite(els)
          }
        }
    }
  }

  def apply[T[_]] = new TraverseProvider[T]

  def list = apply[List]

}
