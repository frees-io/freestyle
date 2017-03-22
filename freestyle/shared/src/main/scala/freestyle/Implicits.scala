package freestyle

import cats.Monad
import cats.arrow.FunctionK
import cats.data.Coproduct
import cats.free.{Free, FreeApplicative, Inject}
import shapeless.Lazy


trait Interpreters {

  implicit def interpretCoproduct[F[_], G[_], M[_]](
      implicit fm: FunctionK[F, M],
      gm: Lazy[FunctionK[G, M]]): FunctionK[Coproduct[F, G, ?], M] =
    fm or gm.value

  implicit def interpretAp[F[_], M[_]: Monad](
      implicit fInterpreter: FunctionK[F, M]): FunctionK[FreeApplicative[F, ?], M] =
    λ[FunctionK[FreeApplicative[F, ?], M]](_.foldMap(fInterpreter))

  // workaround for https://github.com/typelevel/cats/issues/1505
  implicit def catsFreeRightInjectInstanceLazy[F[_], G[_], H[_]](
      implicit I: Lazy[Inject[F, G]]): Inject[F, Coproduct[H, G, ?]] =
    Inject.catsFreeRightInjectInstance(I.value)
}

trait Monads {
  implicit def catsFreeMonadForFreeS[F[_]]: Monad[λ[a => Free[FreeApplicative[F, ?], a]]] =
    Free.catsFreeMonadForFree[FreeApplicative[F, ?]]
}

object implicits extends Interpreters with Monads
