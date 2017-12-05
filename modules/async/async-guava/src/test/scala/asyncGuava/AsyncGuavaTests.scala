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

package freestyle.free.asyncGuava

import java.util.concurrent.{Callable, Executors}

import cats.~>
import com.google.common.util.concurrent.{ListenableFuture, ListeningExecutorService, MoreExecutors}
import org.scalatest._
import freestyle.free.async.implicits._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class AsyncGuavaTests extends WordSpec with Matchers {

  import ExecutionContext.Implicits.global

  val exception: Throwable = new RuntimeException("Test exception")

  val service: ListeningExecutorService =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10))

  def failedFuture[T]: ListenableFuture[T] =
    service.submit(new Callable[T] {
      override def call(): T = throw exception
    })

  def successfulFuture[T](value: T): ListenableFuture[T] =
    service.submit(new Callable[T] {
      override def call(): T = value
    })

  val handler: ListenableFuture ~> Future = implicits.listenableFuture2Async[Future]
  val conv: ListenableFuture[Void] => ListenableFuture[Unit] =
    implicits.listenableVoidToListenableUnit

  val foo = "Bar"

  "Guava ListenableFuture Freestyle integration" should {

    "transform guava ListenableFutures into scala.concurrent.Future successfully" in {
      Await.result(handler(successfulFuture(foo)), Duration.Inf) shouldBe foo
    }

    "recover from failed guava ListenableFutures wrapping them into scala.concurrent.Future" in {
      Await.result(handler(failedFuture[String]).failed, Duration.Inf) shouldBe exception
    }

    "transform guava ListenableFuture[Void] into scala.concurrent.Future successfully through an implicit conversion" in {
      Await.result(handler(conv(successfulFuture[Void](None.orNull))), Duration.Inf) shouldBe ((): Unit)
    }

    "recover from failed guava ListenableFuture[Void] wrapping them into scala.concurrent.Future through an implicit conversion" in {
      Await.result(handler(conv(failedFuture[Void])).failed, Duration.Inf) shouldBe exception
    }

  }

}
