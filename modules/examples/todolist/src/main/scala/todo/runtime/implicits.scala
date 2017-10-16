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
package runtime

import java.util.Properties

import cats._
import cats.effect._
import com.twitter.util._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari._, doobie.hikari.implicits._
import doobie._, doobie.implicits._
import doobie.hikari.imports.HikariTransactor
import todo.runtime.handlers._
import todo.persistence._

object implicits extends ProductionImplicits

/**
 * Implicits representing all the runtime requirements for the Free program declarations to
 * run, It includes implementations for each of the @free algebra handlers and the constrains they imposed
 * over `twitter.util.Future` used in this case to interop with Finch.
 */
trait ProductionImplicits {

  implicit val futureEffectSyncMonadError: cats.MonadError[Future, Throwable] with cats.effect.Sync[
    Future] =
    new MonadError[Future, Throwable] with cats.effect.Sync[Future] {

      override def pure[A](x: A): Future[A] = Future.value(x)

      override def flatMap[A, B](fa: Future[A])(f: (A) => Future[B]): Future[B] = fa flatMap f

      override def suspend[A](thunk: => Future[A]): Future[A] = Future.Unit.flatMap { _ =>
        thunk
      }

      override def raiseError[A](e: Throwable): Future[Nothing] = Future.exception(e)

      override def tailRecM[A, B](a: A)(f: (A) => Future[Either[A, B]]): Future[B] = {
        f(a).map {
          case Left(a1) => tailRecM(a1)(f)
          case Right(c) => Future.value(c)
        }.flatten
      }

      override def handleErrorWith[A](fa: Future[A])(f: (Throwable) => Future[A]): Future[A] =
        fa.rescue { case t => f(t) }

    }

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

  implicit val connectionIO2IO: ConnectionIO ~> IO =
    λ[ConnectionIO ~> IO](_.transact(xa))

  implicit val appRepositoryHandler: AppRepository.Op ~> Future =
    new H2AppRepositoryHandler andThen connectionIO2IO andThen task2Future

  implicit val todoItemRepositoryHandler: TodoItemRepository.Op ~> Future =
    new H2TodoItemRepositoryHandler andThen connectionIO2IO andThen task2Future

  implicit val todoListRepositoryHandler: TodoListRepository.Op ~> Future =
    new H2TodoListRepositoryHandler andThen connectionIO2IO andThen task2Future

  implicit val tagRepositoryHandler: TagRepository.Op ~> Future =
    new H2TagRepositoryHandler andThen connectionIO2IO andThen task2Future

}
