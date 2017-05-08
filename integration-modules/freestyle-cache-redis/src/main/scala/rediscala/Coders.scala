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

package freestyle.cache.redis.rediscala

trait Format[A] extends (A ⇒ String)

object Format {

  def apply[A](print: A ⇒ String) = new Format[A] {
    def apply(a: A): String = print(a)
  }

  implicit val string: Format[String] = new Format[String] {
    def apply(str: String): String = str
  }

}

trait Parser[A] extends (String => Option[A])

object Parser {
  def apply[A](parse: String => Option[A]) = new Parser[A] {
    def apply(s: String): Option[A] = parse(s)
  }

}
