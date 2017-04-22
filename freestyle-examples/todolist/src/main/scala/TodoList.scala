package todolist

package models {
  import doobie.imports._
  import cats._
  import cats.data._
  import cats.implicits._

  import freestyle._
  import freestyle.implicits._

  import freestyle.doobie._
  import freestyle.doobie.implicits._

  object Pong {
    def current: Pong = Pong(System.currentTimeMillis() / 1000L)
  }

  case class Pong(time: Long)

  case class TodoItem(id: Int, item: String)

  object TodoItem {
    val drop = sql"""DROP TABLE todo_items IF EXISTS"""
      .update

    val create = sql"""CREATE TABLE todo_items (id INT AUTO_INCREMENT PRIMARY KEY, item VARCHAR)"""
      .update

    def get(id: Int) = sql"""SELECT id, item FROM todo_items WHERE id = $id"""
      .query[TodoItem]
      .option

    def insert(item: String) = sql"""INSERT INTO todo_items (item) VALUES ($item)"""
      .update

    def list = sql"""SELECT id, item FROM todo_items ORDER BY id ASC"""
      .query[TodoItem]
      .list

    def update(id: Int, item: String) = sql"""UPDATE todo_items SET item = $item WHERE id = $id"""
      .update

    def delete(id: Int) = sql"""DELETE FROM todo_items WHERE id = $id"""
      .update
  }

  @module trait Repository[F[_]] {
    val doobieM: DoobieM[F]
  }

  object Dao {
    def drop[F[_]: DoobieM](implicit repository: Repository[F]): FreeS[F, Int] =
      for {
        rows <- TodoItem.drop.run.liftFS[F]
      } yield rows

    def create[F[_]: DoobieM](implicit repository: Repository[F]): FreeS[F, Int] =
      for {
        rows <- TodoItem.create.run.liftFS[F]
      } yield rows

    def init[F[_]: DoobieM](implicit repository: Repository[F]): FreeS[F, Int] =
      for {
        drops <- drop
        creates <- create
      } yield drops + creates

    def get[F[_]: DoobieM](id: Int)(implicit repository: Repository[F]): FreeS[F, Option[TodoItem]] =
      for {
        todoItem <- TodoItem.get(id).liftFS[F]
      } yield todoItem

    def insert[F[_]: DoobieM](item: String)(implicit repository: Repository[F]): FreeS[F, Int] =
      for {
        rows <- TodoItem.insert(item).run.liftFS[F]
      } yield rows

    def list[F[_]: DoobieM](implicit repository: Repository[F]): FreeS[F, List[TodoItem]] =
      for {
        todoItems <- TodoItem.list.liftFS[F]
      } yield todoItems

    def update[F[_]: DoobieM](id: Int, item: String)(implicit repository: Repository[F]): FreeS[F, Int] =
      for {
        rows <- TodoItem.update(id, item).run.liftFS[F]
      } yield rows

    def delete[F[_]: DoobieM](id: Int)(implicit repository: Repository[F]): FreeS[F, Int] =
      for {
        rows <- TodoItem.delete(id).run.liftFS[F]
      } yield rows
  }
}

package api {
  import models._

  import cats._
  import cats.data._
  import cats.implicits._

  import io.finch._
  import io.finch.circe._
  import io.circe._
  import io.circe.generic.auto._

  import fs2.Task
  import fs2.interop.cats._

  import doobie.imports._
  import doobie.h2.h2transactor._

  import freestyle._
  import freestyle.implicits._
  import freestyle.doobie._
  import freestyle.doobie.implicits._

  object Service {
    implicit val xa = DriverManagerTransactor[Task](
      "org.h2.Driver", "jdbc:h2:mem:freestyle-todo;DB_CLOSE_DELAY=-1", "sa", ""
    )

    val ping: Endpoint[Pong] =
      get("ping") {
        Ok(Pong.current)
      }

    val hello: Endpoint[String] =
      get("hello") {
        Ok("Hello World")
      }

    val genericApi = (hello :+: ping)

    val getTodoItem: Endpoint[Option[TodoItem]] =
      get("items" :: int) { id: Int =>
        Ok(Dao.get[Repository.Op](id).exec[Task].unsafeRunSync.toOption.get)
      }

    val getTodoItems: Endpoint[List[TodoItem]] =
      get("items") {
        Ok(Dao.list[Repository.Op].exec[Task].unsafeRunSync.toOption.get)
      }

    val insertTodoItem: Endpoint[Int] =
      post("items" :: param("item")) { item: String =>
        Ok(Dao.insert[Repository.Op](item).exec[Task].unsafeRunSync.toOption.get)
      }

    val updateTodoItem: Endpoint[Int] =
      put("items" :: int :: param("item")) { (id: Int, item: String) =>
        Ok(Dao.update[Repository.Op](id, item).exec[Task].unsafeRunSync.toOption.get)
      }

    val deleteTodoItem: Endpoint[Int] =
      delete("items" :: int) { id: Int =>
        Ok(Dao.delete[Repository.Op](id).exec[Task].unsafeRunSync.toOption.get)
      }

    val todoItemApi = (getTodoItem :+: getTodoItems :+: insertTodoItem :+: updateTodoItem :+: deleteTodoItem)

    val api = (genericApi :+: todoItemApi)
  }
}

object TodoListApp extends App {
  import api.Service.api
  import com.twitter.finagle.Http
  import com.twitter.util.Await

  import cats._
  import cats.data._
  import cats.implicits._

  import io.finch.circe._
  import io.circe.generic.auto._

  import fs2.Task
  import fs2.interop.cats._

  import doobie.imports._
  import doobie.h2.h2transactor._

  import freestyle._
  import freestyle.implicits._
  import freestyle.doobie._
  import freestyle.doobie.implicits._

  implicit val xa = DriverManagerTransactor[Task](
    "org.h2.Driver", "jdbc:h2:mem:freestyle-todo;DB_CLOSE_DELAY=-1", "sa", ""
  )

  import models._

  Await.ready(Http.server.serve(":8081", api.toService))
}
