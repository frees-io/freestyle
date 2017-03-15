package freestyle.asyncFs2

import freestyle.async._

import fs2.{Strategy, Task}

object implicits {
  implicit def fs2TaskAsyncContext(
      implicit S: Strategy
  ) = new AsyncContext[Task] {
    def runAsync[A](fa: Proc[A]): Task[A] = Task.async(fa)(S)
  }
}
