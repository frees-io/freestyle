/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle.asyncMonix

import freestyle.async._

import monix.eval.Task
import monix.execution.Cancelable

object implicits {
  implicit val monixTaskAsyncContext = new AsyncContext[Task] {
    def runAsync[A](fa: Proc[A]): Task[A] = {
      Task.create { (scheduler, callback) =>
        scheduler.execute(new Runnable {
          def run() = fa(_.fold(callback.onError, callback.onSuccess))
        })
        Cancelable.empty
      }
    }
  }
}
