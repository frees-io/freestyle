package io.freestyle

import cats.{Monad, Applicative}
import cats.free.FreeApplicative
import cats.data.Coproduct
import cats.arrow.FunctionK

trait Interpreters {

  implicit def interpretCoproduct[F[_], G[_], M[_]](implicit fm: FunctionK[F, M], gm: FunctionK[G, M]): FunctionK[Coproduct[F, G, ?], M] =
    fm or gm

  implicit def interpretAp[F[_], M[_]: Monad](implicit fInterpreter: FunctionK[F, M]): FunctionK[FreeApplicative[F, ?], M] =
    new cats.arrow.FunctionK[FreeApplicative[F, ?], M] {
      override def apply[A](fa: FreeApplicative[F, A]): M[A] = fa match {
        case x @ _ => x.foldMap(fInterpreter)
      }
    }
}

