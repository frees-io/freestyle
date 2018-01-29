/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.tagless

import cats.MonadError
import classy.config._
import com.typesafe.config.ConfigFactory
import freestyle.config._

object config {

  @tagless @stacksafe sealed trait ConfigM {
    def load: FS[Config]
    def empty: FS[Config]
    def parseString(s: String): FS[Config]
    def loadAs[T]()(implicit D: ConfigDecoder[T]): FS[T]
    def parseStringAs[T: ConfigDecoder](s: String): FS[T]
  }

  trait Implicits {

    private[config] lazy val underlying = loadConfig(ConfigFactory.load())

    implicit def freestyleConfigHandler[M[_]](
        implicit ME: MonadError[M, Throwable]): ConfigM.Handler[M] =
      new ConfigM.Handler[M] {
        def load: M[Config]  = ME.pure(underlying)
        def empty: M[Config] = ME.pure(loadConfig(ConfigFactory.empty()))
        def parseString(s: String): M[Config] =
          ME.catchNonFatal(loadConfig(ConfigFactory.parseString(s)))
        def loadAs[T]()(implicit D: ConfigDecoder[T]): M[T] =
          toConfigError(D.load()).fold(ME.raiseError, ME.pure)
        def parseStringAs[T](s: String)(implicit decoder: ConfigDecoder[T]): M[T] =
          toConfigError(decoder.fromString(s)).fold(ME.raiseError, ME.pure)
      }
  }

  object implicits extends Implicits

}
