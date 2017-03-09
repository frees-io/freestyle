package freestyle.effects

import freestyle._
import cats.{~>, Foldable, MonadCombine}
import cats.implicits._

object traverse {

  final class TraverseProvider[G[_]] {

    /** Acts as a generator providing traversable semantics to programs
     */
    @free sealed abstract class TraverseM[F[_]] {
      def empty[A]: FreeS.Par[F, A]
      def fromTraversable[A](ta: G[A]): FreeS.Par[F, A]
    }

    /** Interpretable as long as Foldable instance for G[_] and a MonadCombine for M[_] exists
     * in scope
     */
    object implicits {
      implicit def freestyleTraverseMHandler[F[_], M[_]](
          implicit MC: MonadCombine[M],
          FT: Foldable[G]): TraverseM.Handler[M] =
        new TraverseM.Handler[M] {
          def empty[A]: M[A]                     = MC.empty[A]
          def fromTraversable[A](ta: G[A]): M[A] = ta.foldMap(MC.pure)(MC.algebra[A])
        }
    }

    class TraverseFreeSLift[F[_]: TraverseM] extends FreeSLift[F, G] {
      def liftFSPar[A](fa: G[A]): FreeS.Par[F, A] = TraverseM[F].fromTraversable(fa)
    }

    implicit def freeSLiftTraverse[F[_]: TraverseM]: FreeSLift[F, G] =
      new TraverseFreeSLift[F]

  }

  def apply[T[_]] = new TraverseProvider[T]

  def list = apply[List]

}
