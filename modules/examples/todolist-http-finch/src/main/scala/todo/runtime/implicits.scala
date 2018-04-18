/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package examples.todolist
package runtime

import cats._
import cats.effect.IO
import com.twitter.util._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie._
import doobie.hikari._
import doobie.hikari.implicits._
import doobie.implicits._
import examples.todolist.persistence._
import examples.todolist.persistence.runtime._
import java.util.Properties
import scala.concurrent.ExecutionContext

object implicits extends ProductionImplicits

/**
 * Implicits representing all the runtime requirements for the Free program declarations to
 * run, It includes implementations for each of the @free algebra handlers and the constrains they imposed
 * over `twitter.util.Future` used in this case to interop with Finch.
 */
trait ProductionImplicits {

  implicit val xa: HikariTransactor[IO] =
    HikariTransactor[IO](new HikariDataSource(new HikariConfig(new Properties {
      setProperty("driverClassName", "org.h2.Driver")
      setProperty("jdbcUrl", "jdbc:h2:mem:todo")
      setProperty("username", "sa")
      setProperty("password", "")
      setProperty("maximumPoolSize", "10")
      setProperty("minimumIdle", "10")
      setProperty("idleTimeout", "600000")
      setProperty("connectionTimeout", "30000")
      setProperty("connectionTestQuery", "SELECT 1")
      setProperty("maxLifetime", "1800000")
      setProperty("autoCommit", "true")
    })))

  implicit val task2Future: IO ~> Future = new (IO ~> Future) {
    override def apply[A](fa: IO[A]): Future[A] = {
      val promise = new Promise[A]()
      fa.unsafeRunAsync(_.fold(promise.setException, promise.setValue))
      promise
    }
  }

  implicit def appRepositoryHandler[F[_]: Monad](
      implicit T: Transactor[F]): AppRepository.Handler[F] =
    new AppRepositoryHandler[F]

  implicit def todoItemRepositoryHandler[F[_]: Monad](
      implicit T: Transactor[F]): TodoItemRepository.Handler[F] =
    new TodoItemRepositoryHandler[F]

  implicit def todoListRepositoryHandler[F[_]: Monad](
      implicit T: Transactor[F]): TodoListRepository.Handler[F] =
    new TodoListRepositoryHandler[F]

  implicit def tagRepositoryHandler[F[_]: Monad](
      implicit T: Transactor[F]): TagRepository.Handler[F] =
    new TagRepositoryHandler[F]

  implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

}
