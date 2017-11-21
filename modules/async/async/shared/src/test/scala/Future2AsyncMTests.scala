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

package freestyle

import freestyle.async.implicits._
import freestyle.async.Future2AsyncM
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class Future2AsyncMTests extends WordSpec with Matchers {

  val exception: Throwable = new RuntimeException("Future2AsyncMTest exception")

  def failedFuture[T]: Future[T] = Future.failed(exception)

  def successfulFuture[T](value: T): Future[T] = Future.successful(value)

  val F2F: Future2AsyncM[Future] = new Future2AsyncM[Future]

  val foo = "bar"

  "Future2Async Handler" should {

    "transform Future to Future successfully" in {
      Await.result(F2F.apply(successfulFuture(foo)), 5.seconds) shouldBe foo
    }

    "recover from failed Futures and throw them accordingly" in {
      an[Throwable] shouldBe thrownBy(Await.result(F2F.apply(failedFuture[String]), 5.seconds))
    }

  }
}
