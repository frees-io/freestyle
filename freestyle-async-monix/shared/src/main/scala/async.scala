package freestyle.asyncMonix

import cats.{Eval, MonadError}

import freestyle.async._

import scala.util.{Failure, Success, Try}
import scala.concurrent._
import monix.eval.Task
import monix.execution.{Cancelable, Scheduler}

object implicits {
  implicit val taskAsyncContext = new AsyncContext[Task] {
    def runAsync[A](fa: Proc[A]): Task[A] = {
      Task.create((scheduler, callback) => {
        scheduler.execute(new Runnable {
          def run() =
            fa({
              case Success(v)  => callback.onSuccess(v)
              case Failure(ex) => callback.onError(ex)
            })
        })

        Cancelable.empty
      })
    }
  }
}
