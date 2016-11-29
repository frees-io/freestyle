package io.freestyle

import cats.Monad
import cats.free.FreeApplicative
import cats.data.Coproduct
import cats.arrow.FunctionK

trait Interpreters {

  implicit def interpretCoproduct[F[_], G[_], M[_]](implicit fm: FunctionK[F, M], gm: FunctionK[G, M]): FunctionK[Coproduct[F, G, ?], M] =
    fm or gm

  implicit def interpretAp[F[_], M[_]: Monad](implicit fInterpreter: FunctionK[F, M]): FunctionK[FreeApplicative[F, ?], M] =
    λ[FunctionK[FreeApplicative[F, ?], M]](_.foldMap(fInterpreter))
}
