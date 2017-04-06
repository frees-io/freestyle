---
layout: docs
title: Logging
permalink: /docs/patterns/logging/
---

## Logging

The `Logging` effect algebra is part of the `freestyle-logging` module and it allows purely functional logging in the monadic computation flow of a Freestyle program.
It includes most of the well known operations found in loggers as well as different levels to achieve the desired logging output.

In order to enable this integration you may depend on _freestyle-logging_

```scala
libraryDependencies += "com.47deg" %% "freestyle-logging" % "0.1.0"
```

### Operations

The set of abstract operations of the `Logging` algebra are specified as follows.

```scala
@free trait LoggingM[F[_]] {
  def debug(msg: String): FreeS[F, Unit]
  def debugWithCause(msg: String, cause: Throwable): FreeS[F, Unit]
  def error(msg: String): FreeS[F, Unit]
  def errorWithCause(msg: String, cause: Throwable): FreeS[F, Unit]
  def info(msg: String): FreeS[F, Unit]
  def infoWithCause(msg: String, cause: Throwable): FreeS[F, Unit]
  def warn(msg: String): FreeS[F, Unit]
  def warnWithCause(msg: String, cause: Throwable): FreeS[F, Unit]
}
```

Each one of the operations corresponds to variations of `debug`, `error`, `info` and `warn` which cover most use cases when performing logging in an application.

The _freestyle-logging_ module contains built in interpreters for both Scala.jvm and Scala.js which you may use out of the box.
The JVM handler interpreter is based of the [Verizon's Journal Library](https://github.com/Verizon/journal) and the JS handler in [slogging](https://github.com/jokade/slogging)

### Example

In the following example we will show how easy it is to add the logging algebra and use it in a pure program.

Before anything the usual set of imports from freestyle and cats to create our algebras

```tut:silent
import freestyle._
import freestyle.implicits._
import cats._
import cats.implicits._

import scala.util.Try
```

We will define a very simple algebra with a stub handler that returns a list of customer Id's for ilustration purposes.

```tut:book
@free trait CustomerService[F[_]] {
  def customers: FreeS[F, List[String]]
}

implicit val customerServiceHandler: CustomerService.Handler[Try] = new CustomerService.Handler[Try] {
  def customers: Try[List[String]] = Try(List("John", "Jane", "Luis"))
}
```

At this point we may aggregate our customer algebra with any other algebras in a _@module_ which will automatically compose monadic actions
derived from using different algebras.

```tut:book
import freestyle.logging._
import freestyle.loggingJVM.implicits._

@module trait App[F[_]] {
  val customerService: CustomerService[F]
  val log: LoggingM[F]
}
```

And finally we can create a program and compose our logging algebra in the same monadic comprehension along with our customer service.

```tut:book
def program[F[_]](implicit app : App[F]): FreeS[F, Unit] =
  for {
    customers <- app.customerService.customers
	_ <- app.log.debug(s"found customers: $customers")
  } yield ()
```

Once we have a program we can interpret it to our desired runtime, in this case `scala.util.Try`

```tut:evaluated
program[App.Op].exec[Try]
```

### Alternative logging with the `WriterM` effect

The same semantics achieved by the backed in logging algebra can be achieved with traditional MTL style effects that impose a `MonadWriter` constrain in the program's
target monad. Learn more about the writer effect in the [Effects](/docs/effects/#Writer) section.

### A note on performance

Using pure functional logging is ideal but it can affect performance in application's hot paths because Free algebras are reified in memory then interpreted.
While this is a non-issue in most applications whose bottleneck are IO/Network bound you should be aware that traditional logging placed in handlers may yield
better overall performance in specific cases.



