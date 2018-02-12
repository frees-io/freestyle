---
layout: docs
title: HTTP client
permalink: /docs/integrations/hammock/
---

# Hammock HTTP client

[Hammock](http://github.com/pepegar/hammock) is a purely functional
HTTP client for the cats ecosystem.  If you want to read more
documentation on Hammock, you can read [the docs](http://pepegar.com/hammock/docs).

First of all, include the _frees-http-client_ as follows:

[comment]: # (Start Replace)

```scala
libraryDependencies += "io.frees" %% "frees-http-client" % "0.7.0"
```

[comment]: # (End Replace)

## Integration

As always, we should start with the freestyle imports:

```tut:silent
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.http.client._
import freestyle.free.http.client.implicits._
import cats.effect.{Sync, IO}
import _root_.hammock._
import _root_.hammock.Uri._
import _root_.hammock.hi._
import _root_.hammock.jvm.Interpreter

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

```tut
implicit val interp = Interpreter[IO]

HammockM[HammockM.Op].get(Uri.unsafeParse("https://jsonplaceholder.typicode.com/posts/1"), Map.empty[String, String]).interpret[IO].unsafeRunSync
```

Also, you can lift any arbitrary `HttpRequestIO[F]` program to
`HammockM` with `HammockM.run(program)`.

```tut
implicit val interp = Interpreter[IO]
val response = Hammock.getWithOpts(Uri.unsafeParse("https://jsonplaceholder.typicode.com/posts/1"), Opts.empty)
  
HammockM[HammockM.Op].run(response).interpret[IO].unsafeRunSync
```

## Example with modules

```tut
implicit val interp = Interpreter[IO]

def example[F[_] : Example] = for {
  _ <- Example[F].consoleIO.putLine("")
  age <- Example[F].consoleIO.getLine
  emptyHeaders = Map.empty[String, String]
  response <- Example[F].hammockM.get(Uri.unsafeParse("https://jsonplaceholder.typicode.com/posts/1?age=$age"), emptyHeaders)
} yield response

example[Example.Op].interpret[IO].unsafeRunSync
```