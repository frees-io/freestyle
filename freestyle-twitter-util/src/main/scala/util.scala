package freestyle
package twitter

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
