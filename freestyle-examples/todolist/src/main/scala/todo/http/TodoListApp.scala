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

package todo
package http

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch.circe._

import todo.definitions.TodoApp
import todo.http.apis.Api
import todo.runtime.implicits._

object TodoListApp extends TwitterServer {

  val service: Service[Request, Response] = Api.instance[TodoApp.Op].endpoints.toService

  def main(): Unit = {
    val server = Http.server.withAdmissionControl
      .concurrencyLimit(maxConcurrentRequests = 10, maxWaiters = 10)
      .serve(":8081", service)

    onExit { server.close() }

    Await.ready(server)
  }

}
