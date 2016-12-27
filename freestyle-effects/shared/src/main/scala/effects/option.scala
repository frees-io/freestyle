package io.freestyle.effects

import io.freestyle._
import cats.MonadFilter

object option {

  @free sealed trait OptionM[F[_]] {
    def option[A](fa: Option[A]): FreeS.Par[F, A]
    def some[A](a: A): FreeS.Par[F, A]
    def none[A]: FreeS.Par[F, A]
  }

  object implicits {

    implicit def freeStyleOptionMInterpreter[M[_]](
        implicit MF: MonadFilter[M]): OptionM.Interpreter[M] = new OptionM.Interpreter[M] {
      def optionImpl[A](fa: Option[A]): M[A] = fa.map(MF.pure[A]).getOrElse(MF.empty[A])
      def someImpl[A](a: A): M[A]            = MF.pure[A](a)
      def noneImpl[A]: M[A]                  = MF.empty[A]
    }

    class OptionFreeSLift[F[_]: OptionM] extends FreeSLift[F, Option] {
      def liftFSPar[A](fa: Option[A]): FreeS.Par[F, A] = OptionM[F].option(fa)
    }

    implicit def freeSLiftOption[F[_]: OptionM]: FreeSLift[F, Option] = new OptionFreeSLift[F]

  }

}
