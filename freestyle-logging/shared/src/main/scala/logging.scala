package freestyle

object logging {

  @free
  trait LoggingM {

    def debug(msg: String): OpSeq[Unit]

    def debugWithCause(msg: String, cause: Throwable): OpSeq[Unit]

    def error(msg: String): OpSeq[Unit]

    def errorWithCause(msg: String, cause: Throwable): OpSeq[Unit]

    def info(msg: String): OpSeq[Unit]

    def infoWithCause(msg: String, cause: Throwable): OpSeq[Unit]

    def warn(msg: String): OpSeq[Unit]

    def warnWithCause(msg: String, cause: Throwable): OpSeq[Unit]
  }

}
