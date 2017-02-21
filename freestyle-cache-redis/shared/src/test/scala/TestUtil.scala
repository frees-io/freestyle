package freestyle.cache.redis

import cats.arrow.FunctionK
import cats.instances.future
import scala.concurrent.{ExecutionContext, Future}
import freestyle.cache.redis.rediscala._

object TestUtil {

  def redisMap(implicit ec: ExecutionContext): MapWrapper[Future, String, Int] = {
    val format = Format((key: String) => key)
    val parser = Parser((str: String) => Some(str))
    val reader = Deserializers.parser(str => scala.util.Try(Integer.parseInt(str)).toOption)
    val writer = Serializers.printer((age: Int) => age.toString)
    val toM    = FunctionK.id[Future]
    val funcM  = future.catsStdInstancesForFuture(ec)

    new MapWrapper()(format, parser, reader, writer, toM, funcM)
  }

}
