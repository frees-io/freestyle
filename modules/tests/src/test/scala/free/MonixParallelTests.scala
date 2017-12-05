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

package freestyle.free.tests

import freestyle.free.implicits._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MonixParallelTests extends WordSpec with Matchers {
  "Applicative Parallel Support" should {
    "allow non deterministic execution when interpreting to monix.eval.Task" ignore {

      val test = new freestyle.NonDeterminismTestShared
      import test._

      implicit val interpreter = new freestyle.MixedFreeS.Handler[Task] {
        override def x: Task[Int] = Task(blocker(1, 1000L))
        override def y: Task[Int] = Task(blocker(2, 0L))
        override def z: Task[Int] = Task(blocker(3, 2000L))
      }

      Await.result(program.interpret[Task].runAsync, Duration.Inf) shouldBe List(3, 1, 2, 3)
      buf.toArray shouldBe Array(3, 2, 1, 3)
    }
  }
}