package io.freestyle

import cats.{Monad, Applicative}
import cats.free.FreeApplicative
import cats.data.Coproduct
import cats.arrow.FunctionK

trait Interpreters extends InterpretersInstances

trait InterpretersInstances extends InterpreterInstances1 {
  implicit def interpretCoproduct[F[_], G[_], M[_]](implicit fm: FunctionK[F, M], gm: FunctionK[G, M]): FunctionK[Coproduct[F, G, ?], M] =
    fm or gm

  implicit def interpretAp[F[_], M[_]: Monad](implicit freeInterpreter: FunctionK[F, M]): FunctionK[FreeApplicative[F, ?], M] =
    new cats.arrow.FunctionK[FreeApplicative[F, ?], M] {
      override def apply[A](fa: FreeApplicative[F, A]): M[A] = fa match {
        case x @ _ => x.foldMap(freeInterpreter)
      }
    }
}

trait InterpreterInstances1 {
  implicit def interpretEvidence[F[_], G[_], H[_]](implicit FG: FunctionK[F,G], GH: FunctionK[G,H]): FunctionK[F, H] =
    FG andThen GH
}
