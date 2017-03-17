package freestyle
package twitter

import cats.implicits._
import org.scalatest.{Matchers, WordSpec}

import com.twitter.util.{Future, Await, Duration, FuturePool}
import freestyle.implicits._

class TwitterFutureTests extends WordSpec with Matchers {

  "Applicative Parallel Support" should {

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
      import freestyle.twitter.future.implicits._

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
      import freestyle.twitter.future.implicits._

      val test = new NonDeterminismTestShared
      import test._

      implicit val interpreter = new MixedFreeS.Handler[Future] {
        override def x: Future[Int] = Future(blocker(1, 1000L))
        override def y: Future[Int] = Future(blocker(2, 0L))
        override def z: Future[Int] = Future(blocker(3, 2000L))
      }

      Await.result(program.exec[Future], Duration.Top) shouldBe List(3, 1, 2, 3)
      buf.toArray shouldBe Array(3, 1, 2, 3)
    }

  }

}

object algebras {

  @free
  trait MixedFreeS[F[_]] {
    def x: FreeS.Par[F, Int]
    def y: FreeS.Par[F, Int]
    def z: FreeS[F, Int]
  }

}
