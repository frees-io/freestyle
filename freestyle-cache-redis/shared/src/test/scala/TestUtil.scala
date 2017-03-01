package freestyle.cache.redis

import cats.arrow.FunctionK
import cats.instances.future
import scala.concurrent.{ExecutionContext, Future}
import freestyle.cache.redis.rediscala._

object TestUtil {

  def redisMap(implicit ec: ExecutionContext): MapWrapper[Future, String, Int] =
    new MapWrapper()(
      formatKey = Format((key: String) => key),
      parseKey = Parser((str: String) => Some(str)),
      formatVal = Format((age: Int) => age.toString),
      parseVal = Parser((str:String) => scala.util.Try(Integer.parseInt(str)).toOption),
      toM    = FunctionK.id[Future],
      funcM  = future.catsStdInstancesForFuture(ec)
    )

}
