---
layout: docs
title: Logging
permalink: /docs/patterns/logging/
---

## Logging

The `Logging` effect algebra is part of the `freestyle-logging` module, and it allows purely functional logging in the monadic computation flow of a Freestyle program.
It includes most of the well-known operations found in loggers as well as different levels to achieve the desired logging output.

In order to enable this integration, you can depend on _freestyle-logging_:

```scala
libraryDependencies += "com.47deg" %% "freestyle-logging" % "0.1.0"
```

### Operations

The set of abstract operations of the [`Logging` algebra](../../../../../../../freestyle-logging/shared/src/main/scala/logging.scala) are specified as follows:

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

Each operation corresponds to a variations of the typical operations `debug`, `error`, `info`, or `warn`, which cover most use cases when performing logging in an application.

The _freestyle-logging_ module contains built-in handlers, both for Scala.jvm and for Scala.js, which you can use out of the box.
The [JVM handler](../../../../../../../freestyle-logging/jvm/src/main/scala/loggingJVM.scala) is based on the [Verizon's Journal Library](https://github.com/Verizon/journal).
The [JS handler](../../../../../../../freestyle-logging/js/src/main/scala/loggingJS.scala) is based on the [slogging](https://github.com/jokade/slogging) library.

### Example

In the following example, we will show how easy it is to add the logging algebra and use it in a pure program.

Before we do anything else, we need to add the usual set of imports from Freestyle and Cats to create our algebras:

```tut:silent
import freestyle._
import freestyle.implicits._
```

We will use a simple algebra, whose code is available [here](../../../../scala/logging.scala), 
along with a stub handler that returns a list of customer Id's for illustration purposes.

```scala
@free trait CustomerService {
  def customers: FS[List[String]]
}
```
```tut:silent
import freestyle.docs.patterns.CustomerService
```

At this point, we may aggregate our customer algebra with any other algebras in a _@module_ which will automatically compose monadic actions
derived from using different algebras:

```tut:book
import freestyle.logging._
import freestyle.loggingJVM.implicits._

@module trait App[F[_]] {
  val customerService: CustomerService[F]
  val log: LoggingM[F]
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

Once we have a program, we can interpret it to our desired runtime. In this case, we want to use `scala.util.Try` as our runtime. 
For this, we need to  implement a handler `CustomerService.Handler[Try]`, to specify how to interpret our algebra into `Try`. 

```tut:book
import scala.util.Try

implicit val customerServiceHandler: CustomerService.Handler[Try] =
  new CustomerService.Handler[Try] {
    def customers: Try[List[String]] = Try(List("John", "Jane", "Luis"))
  }
```

Finally, to interpret the whole program, we also need an instance of `cats.Monad[Try]`, so we will use the one that `cats` provides. 

```tut:evaluated
import cats.instances.try_._

program[App.Op].exec[Try]
```

### Alternative logging with the `WriterM` effect

The same semantics achieved by the backed in logging algebra can be achieved with traditional MTL style effects that impose a `MonadWriter` constraint in the program's
target monad. Learn more about the writer effect in the [Effects](../../effects/writer) section.

### A note on performance

Using pure functional logging is ideal, but it can affect performance in an application’s hot paths because Free algebras are reified in memory, then interpreted. While this is a non-issue in most applications whose bottleneck are IO/Network bound, you should be aware that traditional logging placed in handlers may yield better overall performance in specific cases.



