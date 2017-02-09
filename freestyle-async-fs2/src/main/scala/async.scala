package freestyle.asyncFs2

import cats.{Eval, MonadError}

import freestyle.async._

import scala.util.{Failure, Success, Try}
import scala.concurrent._
import fs2.{Strategy, Task}

object implicits {
  implicit def fs2TaskAsyncContext(
      implicit S: Strategy
  ) = new AsyncContext[Task] {
    def runAsync[A](fa: Proc[A]): Task[A] = Task.async(fa)(S)
  }
}
