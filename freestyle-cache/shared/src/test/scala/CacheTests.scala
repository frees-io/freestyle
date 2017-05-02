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

package freestyle.cache

import cats.{Applicative, Id, ~>}
import cats.syntax.cartesian._
import freestyle._
import freestyle.implicits._
import org.scalatest.{ Matchers, WordSpec }

class CacheTests extends WordSpec with Matchers with CacheTestContext {

  import cats.implicits._
  import provider.CacheM
  import provider.implicits._
  import CacheM._

  "CacheM algebra" should {

    "allow a CacheM operation to interleaved inside a program monadic flow" in {
      def prog[F[_]: CacheM]: FreeS[F, Int] =
        for {
          a <- FreeS.pure(1)
          b <- CacheM[F].get("a")
          c <- FreeS.pure(1)
        } yield a + b.getOrElse(0) + c
      prog[CacheM.Op].interpret[Id] shouldBe 2
    }
  }

  "GET" should {
    "result in None if the datastore is empty" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].get("a")

      program[CacheM.Op].interpret[Id] shouldBe None
    }

    "result in None if a different key is set" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].put("a", 0) *> CacheM[F].get("b")

      program[CacheM.Op].interpret[Id] shouldBe None
    }

    "result in Some(v) if v is the only set value" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].put("a", 0) *> CacheM[F].get("a")

      program[CacheM.Op].interpret[Id] shouldBe Some(0)
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

      program[CacheM.Op].interpret[Id] shouldBe ((Some(1), 1))
    }

    "ignore succeeding PUT to other Keys" in {
      def program[F[_] : CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("a", 0) *> CacheM[F].put("b", 1)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Id] shouldBe ((Some(0), 2))
    }

    "ignore preceding PUT to other Keys" in {
      def program[F[_] : CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("b", 1) *> CacheM[F].put("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Id] shouldBe ((Some(0), 2))
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

      program[CacheM.Op].interpret[Id] shouldBe ((Some(0), 3))
    }

    "ignore a preceding PUT on same key" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("a", 0) *> CacheM[F].putAll(Map("a"-> 1,"b"-> 1))
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Id] shouldBe ((Some(1), 2))
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

      program[CacheM.Op].interpret[Id] shouldBe ((Some(0), 1))
    }

    "ignore a succeeding PUT on different key" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].put("b", 1) *> CacheM[F].putIfAbsent("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Id] shouldBe ((Some(0), 2))
    }

    "ignore preceding PUTIFABSENT to other Keys" in {
      def program[F[_]: CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].putIfAbsent("b", 1) *> CacheM[F].putIfAbsent("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Id] shouldBe ((Some(0), 2))
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

      program[CacheM.Op].interpret[Id] shouldBe ((None, 0))
    }

    "be overridden by succeeding PUT on same Key" in {
      def program[F[_] : CacheM]: FreeS[F, (Option[Int], Int)] =
        for {
          _ <- CacheM[F].del("a") *> CacheM[F].put("a", 0)
          a <- CacheM[F].get("a")
          k <- CacheM[F].keys
        } yield (a, k.size)

      program[CacheM.Op].interpret[Id] shouldBe ((Some(0), 1))
    }

    "ignore a preceeding PUT on different key" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].put("a", 0) *> CacheM[F].del("b") *> CacheM[F].get("a")

      program[CacheM.Op].interpret[Id] shouldBe Some(0)
    }

    "ignore a succeeding PUT on different key" in {
      def program[F[_] : CacheM]: FreeS[F, Option[Int]] =
        CacheM[F].del("b") *> CacheM[F].put("a", 0) *> CacheM[F].get("a")

      program[CacheM.Op].interpret[Id] shouldBe Some(0)
    }
  }

  "CLEAR" should {
    "remove all the keys" in {
      def program[F[_] : CacheM] =
        for {
          k1 <- CacheM[F].put("a", 0) *> CacheM[F].put("b", 1) *> CacheM[F].keys
          k2 <- CacheM[F].clear *> CacheM[F].keys
        } yield (k1.size, k2.size)

      program[CacheM.Op].interpret[Id] shouldBe ((2, 0))
    }
  }

  "REPLACE" should {
    "replace the entry for a key" in {
      def program[F[_] : CacheM] =
        CacheM[F].put("a", 0) *> CacheM[F].put("b", 1) *> CacheM[F].replace("a", 1) *> CacheM[F].get("a")

      program[CacheM.Op].interpret[Id] shouldBe Some(1)
    }

    // "ignore replace the entry because a key not exist" in {
    //   def program[F[_] : CacheM] =
    //     CacheM[F].put("a", 0) *> CacheM[F].put("b", 1) *> CacheM[F].replace("c", 1) *> CacheM[F].get("c")

    //   program[CacheM.Op].interpret[Id] shouldBe None
    // }
  }

  "ISEMPTY" should {
    "isn't empty" in {
      def program[F[_] : CacheM] =
        CacheM[F].put("a", 0) *> CacheM[F].isEmpty

      program[CacheM.Op].interpret[Id] shouldBe false
    }

    "is empty" in {
      def program[F[_] : CacheM] =
        CacheM[F].put("a", 0) *> CacheM[F].clear *> CacheM[F].isEmpty

      program[CacheM.Op].interpret[Id] shouldBe true
    }
  }

}
