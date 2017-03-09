package freestyle.cache.redis

import cats.{~>, Applicative}
import freestyle._
import freestyle.implicits._
import freestyle.cache.redis.rediscala._
import org.scalatest._
import scala.concurrent.{ExecutionContext, Future}

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
      import cats.implicits._
      def program[F[_]: CacheM]: FreeS[F, Int] =
        for {
          a <- Applicative[FreeS[F, ?]].pure(Some(1))
          b <- (CacheM[F].put("Joe", 13) *> CacheM[F].get("Joe"))
          c <- Applicative[FreeS[F, ?]].pure(Some(1))
        } yield Seq(a, b, c).flatten.sum
      program[CacheM.Op].exec[Future] map { _ shouldBe 15 }
    }

  }
}
