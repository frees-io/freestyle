package freestyle

object logging {

  @free trait LoggingM {

    def debug(msg: String): Oper.Seq[Unit]

    def debugWithCause(msg: String, cause: Throwable): Oper.Seq[Unit]

    def error(msg: String): Oper.Seq[Unit]

    def errorWithCause(msg: String, cause: Throwable): Oper.Seq[Unit]

    def info(msg: String): Oper.Seq[Unit]

    def infoWithCause(msg: String, cause: Throwable): Oper.Seq[Unit]

    def warn(msg: String): Oper.Seq[Unit]

    def warnWithCause(msg: String, cause: Throwable): Oper.Seq[Unit]
  }

}
