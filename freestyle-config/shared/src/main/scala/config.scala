package freestyle

import scala.collection.JavaConverters._
import scala.concurrent.duration._

import cats.MonadError
import com.typesafe.config.ConfigFactory

object config {

  sealed trait Config {
    def hasPath(path: String): Boolean
    def config(path: String): Option[Config]
    def string(path: String): Option[String]
    def boolean(path: String): Option[Boolean]
    def int(path: String): Option[Int]
    def double(path: String): Option[Double]
    def bytes(path: String): Option[Long]
    def stringList(path: String): List[String]
    def duration(path: String, unit: TimeUnit): Option[Long]
    def duration(path: String): Option[Duration]
    def millisDuration(path: String): Option[Duration]
  }

  @free sealed trait ConfigM {
    def load: Oper.Seq[Config]
    def empty: Oper.Seq[Config]
    def parseString(s: String): Oper.Seq[Config]
  }

  object implicits {

    private[config] def loadConfig(shoconC: com.typesafe.config.Config): Config = new Config {

      implicit def asFiniteDuration(d: java.time.Duration): Duration =
        Duration.fromNanos(d.toNanos)

      def hasPath(path: String): Boolean         = shoconC.hasPath(path)
      def config(path: String): Option[Config]   = Option(loadConfig(shoconC.getConfig(path)))
      def string(path: String): Option[String]   = Option(shoconC.getString(path))
      def boolean(path: String): Option[Boolean] = Option(shoconC.getBoolean(path))
      def int(path: String): Option[Int]         = Option(shoconC.getInt(path))
      def double(path: String): Option[Double]   = Option(shoconC.getDouble(path))
      def bytes(path: String): Option[Long]      = Option(shoconC.getBytes(path))
      def stringList(path: String): List[String] = shoconC.getStringList(path).asScala.toList
      def duration(path: String, unit: TimeUnit): Option[Long] =
        Option(shoconC.getDuration(path, unit))
      def duration(path: String): Option[Duration]       = Option(shoconC.getDuration(path))
      def millisDuration(path: String): Option[Duration] = Option(shoconC.getMillisDuration(path))
    }

    lazy val underlying = loadConfig(ConfigFactory.load())

    implicit def freestyleConfigHandler[M[_]](
        implicit ME: MonadError[M, Throwable]): ConfigM.Handler[M] =
      new ConfigM.Handler[M] {
        def load: M[Config]  = ME.pure(underlying)
        def empty: M[Config] = ME.pure(loadConfig(ConfigFactory.empty()))
        def parseString(s: String): M[Config] =
          ME.catchNonFatal(loadConfig(ConfigFactory.parseString(s)))
      }
  }

}
