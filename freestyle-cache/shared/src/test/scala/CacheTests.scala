package freestyle.cache

import cats.{~>, Applicative, Id}
import cats.syntax.cartesian._
import cats.instances.option._
import freestyle._
import freestyle.implicits._
import org.scalatest._

class CacheTests extends WordSpec with Matchers with CacheTestContext {

  import cats.implicits._
  import provider.CacheM
  import provider.implicits._
  import CacheM._

  "GET" should {
    "result in None if the datastore is empty" in {
      def program[F[_]: CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].get("a")

      program[CacheM.T].exec[Id] shouldBe None
    }

    "result in None if a different key is set" in {
      def program[F[_]: CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].put("a", 0) *> CacheM[F].get("b")

      program[CacheM.T].exec[Id] shouldBe None
    }

    "result in Some(v) if v is the only set value" in {
      def program[F[_]: CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].put("a", 0) *> CacheM[F].get("a")

      program[CacheM.T].exec[Id] shouldBe Some(0)
    }
  }

  "PUT" should {

    "reduce consecutive PUT on the same key down to last SET" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- (CacheM[F].put("a", 0) *> CacheM[F].put("a", 1))
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.T].exec[Id] shouldBe ((Some(1), 1))
    }

    "ignore succeeding PUT to other Keys" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("a", 0) *> CacheM[F].put("b", 1)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.T].exec[Id] shouldBe ((Some(0), 2))
    }

    "ignore preceding PUT to other Keys" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("b", 1) *> CacheM[F].put("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.T].exec[Id] shouldBe ((Some(0), 2))
    }
  }

  "DELETE" should {

    "nullify a preceeding PUT on same key" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("a", 0) *> CacheM[F].del("a")
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.T].exec[Id] shouldBe ((None, 0))
    }

    "be overridden by succeeding PUT on same Key" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].del("a") *> CacheM[F].put("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.T].exec[Id] shouldBe ((Some(0), 1))
    }

    "ignore a preceeding PUT on different key" in {
      def program[F[_]: CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].put("a", 0) *> CacheM[F].del("b") *> CacheM[F].get("a")

      program[CacheM.T].exec[Id] shouldBe Some(0)
    }

    "ignore a succeeding PUT on different key" in {
      def program[F[_]: CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].del("b") *> CacheM[F].put("a", 0) *> CacheM[F].get("a")

      program[CacheM.T].exec[Id] shouldBe Some(0)
    }
  }

  "CLEAR" should {
    "remove all the keys" in {
      def program[F[_]: CacheM] =
        for {
          k1 <- (CacheM[F].put("a", 0) *> CacheM[F].put("b", 1) *> CacheM[F].keys)
          k2 <- (CacheM[F].clear *> CacheM[F].keys)
        } yield (k1.size, k2.size)

      program[CacheM.T].exec[Id] shouldBe ((2, 0))
    }
  }

}
