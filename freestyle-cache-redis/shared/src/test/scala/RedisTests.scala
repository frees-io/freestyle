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

package freestyle.cache.redis

import cats.{~>, Applicative}
import freestyle._
import freestyle.implicits._
import freestyle.cache.redis.rediscala._
import org.scalatest._
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._

class RedisTests extends AsyncWordSpec with Matchers with RedisTestContext {

  implicit override def executionContext = ExecutionContext.Implicits.global

  private[this] val provider = freestyle.cache.apply[String, Int]

  import provider.CacheM
  import provider.implicits._

  implicit val redisMap: MapWrapper[Future, String, Int] = TestUtil.redisMap

  implicit val interpreter: Ops[Future, ?] ~> Future =
    new Interpret[Future](client)

  "Redis integration into Freestyle" should {

    "allow to interleave one CacheM within the programs monadic flow" in {
      def program[F[_]: CacheM]: FreeS[F, Int] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(Some(1))
          b <- CacheM[F].put("Joe", 13) *> CacheM[F].get("Joe")
          c <- Applicative[FreeS[F, ?]].pure(Some(1))
        } yield Seq(a, b, c).flatten.sum
      program[CacheM.Op].interpret[Future] map { _ shouldBe 15 }
    }
  }

  "GET" should {

    "result in None if the datastore is empty" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].get("a")

      program[CacheM.Op].interpret[Future] map { _ shouldBe None }
    }

    "result in None if a different key is set" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].put("a", 0) *> CacheM[F].get("b")

      program[CacheM.Op].interpret[Future] map { _ shouldBe None }
    }

    "result in Some(v) if v is the only set value" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].put("a", 0) *> CacheM[F].get("a")

      program[CacheM.Op].interpret[Future] map { _ shouldBe Some(0)}
    }
  }

  "PUT" should {

    "reduce consecutive PUT on the same key down to last SET" in {
      def program[F[_] : CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- (CacheM[F].put("a", 0) *> CacheM[F].put("a", 1))
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Future] map { _ shouldBe ((Some(1), 1)) }
    }

    "ignore succeeding PUT to other Keys" in {
      def program[F[_] : CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("a", 0) *> CacheM[F].put("b", 1)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Future] map { _ shouldBe ((Some(0), 2)) }
    }

    "ignore preceding PUT to other Keys" in {
      def program[F[_] : CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("b", 1) *> CacheM[F].put("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Future] map { _ shouldBe ((Some(0), 2)) }
    }
  }

  "PUTALL" should {

      "ignore succeeding PUT to other Keys" in {
        def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
          for {
            _ <- CacheM[F].putAll(Map("a"-> 0,"b"-> 1)) *> CacheM[F].put("c", 2)
            a <- CacheM[F].get("a")
            k <- CacheM[F].keys
          } yield (a, k.size)

        program[CacheM.Op].interpret[Future] map {_ shouldBe ((Some(0), 3)) }
    }

    "ignore a preceding PUT on same key" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("a", 0) *> CacheM[F].putAll(Map("a"-> 1,"b"-> 1))
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Future] map { _ shouldBe ((Some(1), 2))}
    }
  }

  "PUTIFABSENT" should {

    "ignore a PUTIFABSENT because the key is already associated with a value" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("a", 0) *> CacheM[F].putIfAbsent("a", 1)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Future] map { _ shouldBe ((Some(0), 1))}
    }

    "ignore a succeeding PUT on different key" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("b", 1) *> CacheM[F].putIfAbsent("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Future] map { _ shouldBe ((Some(0), 2))}
    }

    "ignore preceding PUTIFABSENT to other Keys" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].putIfAbsent("b", 1) *> CacheM[F].putIfAbsent("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Future] map { _  shouldBe ((Some(0), 2))}
    }
  }

  "DELETE" should {

    "nullify a preceeding PUT on same key" in {
      def program[F[_] : CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("a", 0) *> CacheM[F].del("a")
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Future] map { _  shouldBe ((None, 0)) }
    }

    "be overridden by succeeding PUT on same Key" in {
      def program[F[_] : CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].del("a") *> CacheM[F].put("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Future] map { _  shouldBe ((Some(0), 1)) }
    }

    "ignore a preceeding PUT on different key" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].put("a", 0) *> CacheM[F].del("b") *> CacheM[F].get("a")

      program[CacheM.Op].interpret[Future] map { _  shouldBe Some(0) }
    }

    "ignore a succeeding PUT on different key" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].del("b") *> CacheM[F].put("a", 0) *> CacheM[F].get("a")

      program[CacheM.Op].interpret[Future] map { _  shouldBe Some(0) }
    }
  }

  "CLEAR" should {
    "remove all the keys" in {
      def program[F[_] : CacheM] =
        for {
          k1 <- CacheM[F].put("a", 0) *> CacheM[F].put("b", 1) *> CacheM[F].keys
          k2 <- CacheM[F].clear *> CacheM[F].keys
        } yield (k1.size, k2.size)

      program[CacheM.Op].interpret[Future] map { _  shouldBe ((2, 0)) }
    }
  }

  "REPLACE" should {
    "replace the entry for a key" in {
      def program[F[_] : CacheM] =
        CacheM[F].put("a", 0) *> CacheM[F].put("b", 1) *> CacheM[F].replace("a", 1) *> CacheM[F].get("a")

      program[CacheM.Op].interpret[Future] map { _  shouldBe Some(1) }
    }

    "ignore replace the entry because a key not exist" in {
      def program[F[_] : CacheM] =
        CacheM[F].put("a", 0) *> CacheM[F].put("b", 1) *> CacheM[F].replace("c", 1) *> CacheM[F].get("c")

      program[CacheM.Op].interpret[Future] map { _  shouldBe None }
    }
  }

  "ISEMPTY" should {
    "isn't empty" in {
      def program[F[_] : CacheM] =
        CacheM[F].put("a", 0) *> CacheM[F].isEmpty

      program[CacheM.Op].interpret[Future] map { _  shouldBe false }
    }

    "is empty" in {
      def program[F[_] : CacheM] =
        CacheM[F].put("a", 0) *> CacheM[F].clear *> CacheM[F].isEmpty

      program[CacheM.Op].interpret[Future] map { _  shouldBe true }
    }
  }
}
