package freestyle
package twitter

import com.twitter.util._
import io.catbird.util._

object util {

  object implicits extends FutureInstances with TryInstances with VarInstances {

    implicit val freestyleCaptureForTwitterFuture: Capture[Future] = new Capture[Future] {
      def capture[A](a: => A) = Future(a)
    }

    implicit val freestyleCaptureForTwitterTry: Capture[Try]  = new Capture[Try] {
      def capture[A](a: => A) = Try(a)
    }

    implicit val freestyleCaptureForTwitterVar: Capture[Var] = new Capture[Var] {
      def capture[A](a: => A) = Var(a)
    }
    
  }

}
