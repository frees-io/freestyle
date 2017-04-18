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
    def stringList(path: String): List[String]
    def duration(path: String, unit: TimeUnit): Option[Long]
  }

  @free sealed trait ConfigM[F[_]] {
    def load: FreeS[F, Config]
    def empty: FreeS[F, Config]
    def parseString(s: String): FreeS[F, Config]
  }

  object implicits {

    private[config] def loadConfig(c: com.typesafe.config.Config): Config = new Config {

      def hasPath(path: String): Boolean         = c.hasPath(path)
      def config(path: String): Option[Config]   = Option(loadConfig(c.getConfig(path)))
      def string(path: String): Option[String]   = Option(c.getString(path))
      def boolean(path: String): Option[Boolean] = Option(c.getBoolean(path))
      def int(path: String): Option[Int]         = Option(c.getInt(path))
      def double(path: String): Option[Double]   = Option(c.getDouble(path))
      def stringList(path: String): List[String] = c.getStringList(path).asScala.toList
      def duration(path: String, unit: TimeUnit): Option[Long] =
        Option(c.getDuration(path, unit))

    }

    private[config] lazy val underlying = loadConfig(ConfigFactory.load())

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
