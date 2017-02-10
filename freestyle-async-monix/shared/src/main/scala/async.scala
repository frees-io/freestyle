package freestyle.asyncMonix

import cats.{Eval, MonadError}

import freestyle.async._

import scala.concurrent._
import monix.eval.Task
import monix.execution.{Cancelable, Scheduler}

object implicits {
  implicit val monixTaskAsyncContext = new AsyncContext[Task] {
    def runAsync[A](fa: asynProc[A]): Task[A] = {
      Task.create((scheduler, callback) => {
        scheduler.execute(new Runnable {
          def run() =
            fa((result: Either[Throwable, A]) =>
              result match {
                case Right(v) => callback.onSuccess(v)
                case Left(ex) => callback.onError(ex)
            })
        })

        Cancelable.empty
      })
    }
  }
}
