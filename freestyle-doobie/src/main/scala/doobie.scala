package io.freestyle

import cats.{Foldable, ~>}
import io.freestyle._
import _root_.doobie.imports._

object doobie {

  @free trait Persistence[F[_]] {

    def list[A: Composite](sql: String): FreeS[F, List[A]]

    def listFor[A: Composite, L: Composite](sql: String, values: A): FreeS[F, List[L]]

    def option[A: Composite, L: Composite](sql: String, values: A): FreeS[F, Option[L]]

    def unique[A: Composite, L: Composite](sql: String, values: A): FreeS[F, L]

    def update(sql: String): FreeS[F, Int]

    def updateFor[A: Composite](sql: String, values: A): FreeS[F, Int]

    def updateMany[A: Composite](sql: String, values: List[A]): FreeS[F, Int]

    def updateWithKeys[A: Composite, L: Composite](sql: String, fields: List[String], values: A): FreeS[F, L]

  }

  implicit val freeStyleDoobieInterpreter: Persistence.Interpreter[ConnectionIO] =
    new Persistence.Interpreter[ConnectionIO] {

      import shapeless.HNil

      val HNilComposite = implicitly[Composite[HNil]]

      def listImpl[A](sql: String, ac: Composite[A]): ConnectionIO[List[A]] =
        Query[HNil, A](sql)(HNilComposite, ac).toQuery0(HNil).to[List]

      def listForImpl[A, L](sql: String, values: A, ac: Composite[A], lc: Composite[L]): ConnectionIO[List[L]] =
        Query[A, L](sql)(ac, lc).to[List](values)

      def optionImpl[A, L](sql: String, values: A, ac: Composite[A], lc: Composite[L]): ConnectionIO[Option[L]] =
        Query[A, L](sql)(ac, lc).option(values)

      def uniqueImpl[A, L](sql: String, values: A, ac: Composite[A], lc: Composite[L]): ConnectionIO[L] =
        Query[A, L](sql)(ac, lc).unique(values)

      def updateImpl(sql: String): ConnectionIO[Int] =
        Update[HNil](sql).run(HNil)

      def updateForImpl[A](sql: String, values: A, ac: Composite[A]): ConnectionIO[Int] = {
        Update[A](sql)(ac).run(values)
      }

      def updateManyImpl[A](sql: String, values: List[A], ac: Composite[A]): ConnectionIO[Int] = {
        import cats.implicits._
        Update[A](sql)(ac).updateMany(values)(Foldable[List])
      }

      
      def updateWithKeysImpl[A, L](sql: String, fields: List[String], values: A, ac: Composite[A], lc: Composite[L]): ConnectionIO[L] =
        Update[A](sql)(ac).withUniqueGeneratedKeys[L](fields: _*)(values)(lc)
    }

}

