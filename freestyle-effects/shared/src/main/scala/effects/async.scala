package freestyle.effects

import freestyle._
import cats.{Eval, MonadError}

import scala.util.{Failure, Success, Try}
import scala.concurrent._

object async {
  type Proc[A] = (Try[A] => Unit) => Unit

  trait AsyncContext[M[_]] {
    def runAsync[A](fa: Proc[A]): M[A]
  }

  @free sealed trait AsyncM[F[_]] {
    def async[A](fa: Proc[A]): FreeS.Par[F, A]
  }

  object implicits {

    implicit def futureAsyncContext(
        implicit ex: ExecutionContext
    ) = new AsyncContext[Future] {
      def runAsync[A](fa: Proc[A]): Future[A] = {
        val p = Promise[A]()

        ex.execute(new Runnable {
          def run() =
            fa({
              case Success(v)  => p.trySuccess(v)
              case Failure(ex) => p.tryFailure(ex)
            })
        })

        p.future
      }
    }

    // todo: Task

    implicit def freeStyleAsyncMInterpreter[M[_]](
        implicit MA: AsyncContext[M]
    ): AsyncM.Interpreter[M] =
      new AsyncM.Interpreter[M] {
        def asyncImpl[A](fa: Proc[A]): M[A] =
          MA.runAsync(fa)
      }
  }
}
