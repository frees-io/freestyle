---
layout: docs
title: Logging
permalink: /docs/patterns/logging/
---

## Logging

The `Logging` effect algebra is part of the `freestyle-logging` module, and it allows purely functional logging in the monadic computation flow of a Freestyle program.
It includes most of the well-known operations found in loggers as well as different levels to achieve the desired logging output.

In order to enable this integration, you can depend on _freestyle-logging_:

[comment]: # (Start Replace)

```scala
libraryDependencies += "com.47deg" %% "freestyle-logging" % "0.1.0"
```

[comment]: # (End Replace)

### Operations

The set of abstract operations of the `Logging` algebra are specified as follows:

```scala
@free trait LoggingM {
  def debug(msg: String): FS[Unit]
  def debugWithCause(msg: String, cause: Throwable): FS[Unit]
  def error(msg: String): FS[Unit]
  def errorWithCause(msg: String, cause: Throwable): FS[Unit]
  def info(msg: String): FS[Unit]
  def infoWithCause(msg: String, cause: Throwable): FS[Unit]
  def warn(msg: String): FS[Unit]
  def warnWithCause(msg: String, cause: Throwable): FS[Unit]
}
```

Each one of the operations corresponds to variations of `debug`, `error`, `info`, and `warn` which cover most use cases when performing logging in an application.

The _freestyle-logging_ module contains built-in interpreters for both Scala.jvm and Scala.js which you may use out of the box.
The JVM handler interpreter is based on the [Verizon's Journal Library](https://github.com/Verizon/journal) and the JS handler in [slogging](https://github.com/jokade/slogging).

### Example

In the following example, we will show how easy it is to add the logging algebra and use it in a pure program.

Before we do anything else, we need to add the usual set of imports from Freestyle and Cats to create our algebras:

```tut:silent
import freestyle._
import freestyle.implicits._
import cats._
import cats.implicits._

import scala.util.Try
```

We will define a simple algebra with a stub handler that returns a list of customer Id's for illustration purposes:

```tut:book
@free trait CustomerService {
  def customers: FS[List[String]]
}

implicit val customerServiceHandler: CustomerService.Handler[Try] = new CustomerService.Handler[Try] {
  def customers: Try[List[String]] = Try(List("John", "Jane", "Luis"))
}
```

At this point, we may aggregate our customer algebra with any other algebras in a _@module_ which will automatically compose monadic actions
derived from using different algebras:

```tut:book
import freestyle.logging._
import freestyle.loggingJVM.implicits._

@module trait App {
  val customerService: CustomerService
  val log: LoggingM
}
```

And finally, we can create a program and compose our logging algebra in the same monadic comprehension along with our customer service:

```tut:book
def program[F[_]](implicit app : App[F]): FreeS[F, Unit] =
  for {
    customers <- app.customerService.customers
	_ <- app.log.debug(s"found customers: $customers")
  } yield ()
```

Once we have a program, we can interpret it to our desired runtime, in this case, `scala.util.Try`:

```tut:evaluated
program[App.Op].interpret[Try]
```

### Alternative logging with the `WriterM` effect

The same semantics achieved by the backed in logging algebra can be achieved with traditional MTL style effects that impose a `MonadWriter` constraint in the program's
target monad. Learn more about the writer effect in the [Effects](../../effects/writer) section.

### A note on performance

Using pure functional logging is ideal, but it can affect performance in an application’s hot paths because Free algebras are reified in memory, then interpreted. While this is a non-issue in most applications whose bottleneck are IO/Network bound, you should be aware that traditional logging placed in handlers may yield better overall performance in specific cases.



