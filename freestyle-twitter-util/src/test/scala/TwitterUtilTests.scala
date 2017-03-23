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

package freestyle
package twitter

import cats.implicits._
import org.scalatest.{Matchers, WordSpec}

import com.twitter.util._
import freestyle.implicits._

class TwitterUtilTests extends WordSpec with Matchers {

  "Twitter util interpretation" should {

    import algebras._

    class NonDeterminismTestShared {

      val buf = scala.collection.mutable.ArrayBuffer.empty[Int]

      def blocker(value: Int, waitTime: Long): Int = {
        Thread.sleep(waitTime)
        buf += value
        value
      }

      val v = MixedFreeS[MixedFreeS.Op]
      import v._

      val program = for {
        a  <- z //3
        bc <- (x |@| y).tupled.freeS //(1,2)
        (b, c) = bc
        d <- z //3
      } yield a :: b :: c :: d :: Nil // List(3,1,2,3)

    }

    "allow non deterministic execution when interpreting to twitter.util.Future" in {
      import freestyle.twitter.util.implicits._

      val test = new NonDeterminismTestShared
      import test._

      val futurePool = FuturePool.unboundedPool

      implicit val interpreter = new MixedFreeS.Handler[Future] {
        override def x: Future[Int] = futurePool(blocker(1, 1000L))
        override def y: Future[Int] = futurePool(blocker(2, 0L))
        override def z: Future[Int] = futurePool(blocker(3, 2000L))
      }

      Await.result(program.exec[Future], Duration.Top) shouldBe List(3, 1, 2, 3)
      buf.toArray shouldBe Array(3, 2, 1, 3)
    }

    "allow deterministic execution when interpreting to twitter.util.Future" in {
      import freestyle.twitter.util.implicits._
      import captureInterpreters._

      val test = new NonDeterminismTestShared
      import test._

      Await.result(program.exec[Future], Duration.Top) shouldBe List(3, 1, 2, 3)
    }

    "allow execution when interpreting to twitter.util.Try" in {
      import freestyle.twitter.util.implicits._
      import captureInterpreters._

      val test = new NonDeterminismTestShared
      import test._

      program.exec[Try] shouldBe Return(List(3, 1, 2, 3))
    }

  }

}

object algebras {

  @free
  trait MixedFreeS {
    def x: OpPar[Int]
    def y: OpPar[Int]
    def z: OpSeq[Int]
  }

}

object captureInterpreters {

  import algebras._

  implicit def interpreter[M[_]](implicit C: Capture[M]): MixedFreeS.Handler[M] =
    new MixedFreeS.Handler[M] {
      override def x: M[Int] = C.capture(1)
      override def y: M[Int] = C.capture(2)
      override def z: M[Int] = C.capture(3)
    }

}
