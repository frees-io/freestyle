---
layout: docs
title: HTTP client
permalink: /docs/integrations/hammock/
---

# Hammock HTTP client

[Hammock](http://pepegar.com/master) is a purely functional HTTP
client for the cats ecosystem.  You can easily integrate it within
your Freestyle programs.

First of all, include the _frees-http-client_ as follows:

[comment]: # (Start Replace)

```scala
libraryDependencies += "io.frees" %% "frees-http-client" % "0.4.1"
```

[comment]: # (End Replace)

## Integration

As always, we should start with the freestyle imports:

```tut:silent
import freestyle._
import freestyle.implicits._
import freestyle.http.client._
import freestyle.http.client.implicits._
import cats.effect.{Sync, IO}
import _root_.hammock._
import _root_.hammock.Uri._
import _root_.hammock.hi._
import _root_.hammock.jvm.free.Interpreter

```

Let's start with a simple algebra that will allow us to do console IO:

```tut:silent
@free trait ConsoleIO {
  def putLine(text: String): FS[Unit]
  def getLine: FS[String]
}

implicit val consoleIOSyncHandler = new ConsoleIO.Handler[IO] {
  def putLine(text: String): IO[Unit] = Sync[IO].delay(println(text))
  def getLine: IO[String] = Sync[IO].delay("30")
}

@module trait Example {
  val hammockM: HammockM
  val consoleIO: ConsoleIO
}
```

There are two different ways to integrate your Hammock programs in
your Freestyle flow.  First of all, you can use the methods in the
`HammockM` algebra that represent HTTP verbs:

```tut:silent
def f[F[_] : Example] = Example[F].hammockM.get(Uri.unsafeParse("https://jsonplaceholder.typicode.com"), Map.empty[String, String])
```

Also, you can lift any arbitrary `HttpRequestIO[F]` program to
`HammockM` with `HammockM.run(program)`.

```tut:silent
val response = Hammock.getWithOpts(Uri.unsafeParse("https://jsonplaceholder.typicode.com"), Opts.default)
  
def f[F[_] : Example] = Example[F].hammockM.run(response)
```

## Example with modules

```tut:silent
implicit val interp = Interpreter[IO]

def example[F[_] : Example] = for {
  _ <- Example[F].consoleIO.putLine("")
  age <- Example[F].consoleIO.getLine
  emptyHeaders = Map.empty[String, String]
  response <- Example[F].hammockM.get(Uri.unsafeParse("https://jsonplaceholder.typicode.com?age=$age"), emptyHeaders)
} yield response

example[Example.Op].interpret[IO]
```
