package freestyle.effects

import freestyle._
import cats.{Eval, MonadError}

import scala.concurrent._

object async {
  // todo: move to Try[A] so errors can be signaled

  type Proc[A] = (A => Unit) => Unit

  trait Async[M[_]] {
    def runAsync[A](fa: Proc[A]): M[A]
  }

  @free sealed trait AsyncM[F[_]] {
    def async[A](fa: Proc[A]): FreeS.Par[F, A]
  }

  object implicits {

    implicit def futureAsync(
        implicit ex: ExecutionContext
    ) = new Async[Future] {
      def runAsync[A](fa: Proc[A]): Future[A] = {
        val p = Promise[A]()

        ex.execute(new Runnable {
          def run() = fa(p.trySuccess)
        })

        p.future
      }
    }

    implicit def freeStyleAsyncMInterpreter[M[_]](implicit MA: Async[M]): AsyncM.Interpreter[M] =
      new AsyncM.Interpreter[M] {
        def asyncImpl[A](fa: Proc[A]): M[A] =
          MA.runAsync(fa)
      }
  }
}
