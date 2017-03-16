package freestyle
package effects

import cats.MonadFilter

object option {

  @free sealed trait OptionM {
    def option[A](fa: Option[A]): Oper.Par[A]
    def none[A]: Oper.Par[A]
  }

  trait OptionImplicits {
    implicit def freeStyleOptionMHandler[M[_]](
        implicit MF: MonadFilter[M]): OptionM.Handler[M] = new OptionM.Handler[M] {
      def option[A](fa: Option[A]): M[A] = fa.map(MF.pure[A]).getOrElse(MF.empty[A])
      def none[A]: M[A]                  = MF.empty[A]
    }

    class OptionFreeSLift[F[_]: OptionM.To] extends FreeSLift[F, Option] {
      def liftFSPar[A](fa: Option[A]): FreeS.Par[F, A] = OptionM[F].option(fa)
    }

    implicit def freeSLiftOption[F[_]: OptionM.To]: FreeSLift[F, Option] = new OptionFreeSLift[F]
  }

  object implicits extends OptionImplicits
}
