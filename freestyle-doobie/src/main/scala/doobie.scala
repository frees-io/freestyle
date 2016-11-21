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

  implicit def ConnectionIO2M[M[_]](implicit tx: Transactor[M]): ConnectionIO ~> M =
    λ[ConnectionIO ~> M](_.transact(tx))

}

object algebras extends App {

  @free trait Algebra2[F[_]] {
    def x: FreeS[F, Int]
  }

  implicit val a2Interpreter: Algebra2.Interpreter[IOLite] = new Algebra2.Interpreter[IOLite] {
    def xImpl = IOLite.primitive(3)
  }

}

object modules {

  import doobie._
  import algebras._

  @module trait Combined[F[_]] {
    val persistence: Persistence[F]
    val algebra2: Algebra2[F]
  }

}

object Run extends App {

  import modules._
  import _root_.doobie.imports._
  import io.freestyle.implicits._
  import doobie._

  implicit val x: Transactor[IOLite] = ???

  implicit val ioLiteMonad = new cats.Monad[IOLite] {
    def flatMap[A, B](fa: IOLite[A])(f: A => IOLite[B]): IOLite[B] = fa.flatMap(f)
    def pure[A](a: A): IOLite[A] = IOLite.pure(a)

    final def tailRecM[B, C](b: B)(f: B => IOLite[Either[B, C]]): IOLite[C] =
      f(b).flatMap {
        case Left(b1) => tailRecM(b1)(f)
        case Right(c) => IOLite.pure(c)
      }

  }

  def program[F[_]](implicit C: Combined[F]) = {
    for {
      x <- C.algebra2.x
      y <- C.persistence.list[shapeless.HNil]("select 1 + 1")
    } yield (x, y)
  }

  program[Combined.T].exec[IOLite]

}

/*
  def generateQuery(sql: String): Query0[K] =
    Query[HNil, K](sql).toQuery0(HNil)

  class GenerateQuery[L] {
    def apply[A: Composite](sql: String, values: A)(implicit L: Composite[L]): Query0[L] =
      Query[A, L](sql).toQuery0(values)
  }

  def generateQuery = new GenerateQuery[K]

  def generateQueryFor[L] = new GenerateQuery[L]

  def generateUpdateWithGeneratedKeys[A: Composite](sql: String, values: A): Update0 =
    Update[A](sql).toUpdate0(values)

  def generateUpdate(sql: String): Update0 = Update0(sql, None)

  class FetchList[L] {
    def apply(sql: String)(implicit L: Composite[L]): ConnectionIO[List[L]] =
      Query[HNil, L](sql).toQuery0(HNil).to[List]

    def apply[A: Composite](sql: String, values: A)(implicit L: Composite[L]): ConnectionIO[List[L]] =
      Query[A, L](sql).to[List](values)
  }

  def fetchList = new FetchList[K]

  def fetchListAs[L] = new FetchList[L]

  def fetchOption[A: Composite](sql: String, values: A): ConnectionIO[Option[K]] =
    Query[A, K](sql).option(values)

  def fetchUnique[A: Composite](sql: String, values: A): ConnectionIO[K] =
    Query[A, K](sql).unique(values)

  def update(sql: String): ConnectionIO[Int] = Update[HNil](sql).run(HNil)

  def update[A](sql: String, values: A)(implicit A: Composite[A]): ConnectionIO[Int] =
    Update[A](sql).run(values)

  class UpdateWithGeneratedKeys[L] {

    def apply[A: Composite](sql: String, fields: List[String], values: A)(implicit K: Composite[L]): ConnectionIO[L] = {
      val prefix = if (supportsSelectForUpdate) fields else List("id")
      Update[A](sql).withUniqueGeneratedKeys[L](prefix: _*)(values)
    }
  }

  def updateWithGeneratedKeys[L] = new UpdateWithGeneratedKeys[L]

  def updateMany[F[_]: Foldable, A: Composite](sql: String, values: F[A]): ConnectionIO[Int] =
    Update[A](sql).updateMany(values)
 */
