package freestyle.cache.redis

import cats.arrow.FunctionK
import cats.instances.future
import scredis._
import scala.concurrent.{ExecutionContext, Future}

object TestUtil {

  def redisMap(implicit ec: ExecutionContext): RedisMapWrapper[Future, String, Int] = {
    val format = Format((key: String) => key)
    val reader = Readers.parser(str => scala.util.Try(Integer.parseInt(str)).toOption)
    val writer = Writers.printer((age: Int) => age.toString)
    val toM    = FunctionK.id[Future]
    val funcM  = future.catsStdInstancesForFuture(ec)

    new RedisMapWrapper()(format, reader, writer, toM, funcM)
  }

}
