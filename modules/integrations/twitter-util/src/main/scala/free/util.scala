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

package freestyle.free
package twitter

import freestyle.free._
import com.twitter.util._
import io.catbird.util._

object util {

  object implicits extends FutureInstances with TryInstances {

    implicit val freestyleCaptureForTwitterFuture: Capture[Future] = new Capture[Future] {
      def capture[A](a: => A) = Future(a)
    }

    implicit val freestyleCaptureForTwitterTry: Capture[Try] = new Capture[Try] {
      def capture[A](a: => A) = Try(a)
    }

  }

}
