package freestyle

import cats.{Eval, MonadError}

import scala.concurrent._

object async {

  /** An asynchronous computation that might fail. **/
  type Proc[A] = (Either[Throwable, A] => Unit) => Unit

  /** The context required to run an asynchronous computation. **/
  trait AsyncContext[M[_]] {
    def runAsync[A](fa: Proc[A]): M[A]
  }

  /** Async computation algebra. **/
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
              case Right(v) => p.trySuccess(v)
              case Left(ex) => p.tryFailure(ex)
            })
        })

        p.future
      }
    }

    implicit def freeStyleAsyncMHandler[M[_]](
        implicit MA: AsyncContext[M]
    ): AsyncM.Handler[M] =
      new AsyncM.Handler[M] {
        def asyncImpl[A](fa: Proc[A]): M[A] =
          MA.runAsync(fa)
      }
  }
}
