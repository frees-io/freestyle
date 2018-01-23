---
layout: docs
title: http4s
permalink: /docs/integrations/http4s/
---

# http4s

A Freestyle program can easily be used with [http4s](http://http4s.org/).

You can add the _frees-http4s_ module as follows:

[comment]: # (Start Replace)

```scala
libraryDependencies += "io.frees" %% "frees-http4s" % "0.6.3"
```

[comment]: # (End Replace)

Note that Freestyle only supports the http4s version based on Cats and FS2.

## Integration

The _frees-http4s_ module allows you to return a `FreeS[F, A]` value if there is an `F.Handler[G]` in scope and an `EntityEncoder[H, G[A]]`. Http4s provides `EntityEncoder` instances for `cats.effect.IO`, `Future`, `Id`, etc out of the box.

## Example

First, lets import all the regular freestyle imports:

```tut:silent
import freestyle.free._
import freestyle.free.implicits._
```

And the specific import for `frees-http4s`:

```tut:silent
import freestyle.free.http.http4s._
```

In this example, we will create an algebra to calculate the VAT of a product's price:

```tut:book
import scala.math.BigDecimal

@free trait CalcVAT {
  def vat(price: BigDecimal): FS[BigDecimal]
  def withVat(price: BigDecimal): FS[BigDecimal] =
    vat(price).map(_ + price)
}
```

A handler for the `CalcVAT` algebra with `fs2.Task` as target type:

```tut:book
import cats.effect.IO

implicit val calcIoHandler = new CalcVAT.Handler[IO] {
  private val rate: BigDecimal = BigDecimal(20) / BigDecimal(100)
  def vat(price: BigDecimal): IO[BigDecimal] =
    IO.pure(price * rate)
}
```

Some imports for http4s:

```tut:silent
import org.http4s._
import org.http4s.dsl.io._
```

We can now create a simple `HttpService`:

```tut:book
object Price {
  def unapply(s: String): Option[BigDecimal] =
    if (s.isEmpty) None else Some(BigDecimal(s))
}

val userService = HttpService[IO] {
  case GET -> Root / "calc-vat" / Price(p) =>
    Ok(CalcVAT[CalcVAT.Op].vat(p).map(vat => s"The VAT for $p is $vat"))
  case GET -> Root / "calc-total" / Price(p) =>
    Ok(CalcVAT[CalcVAT.Op].withVat(p).map(total => s"The total price including VAT is $total"))
}
```

Trying out our `HttpService`:

```tut:book
val getVAT = Request[IO](Method.GET, uri("/calc-vat/100.50"))
val getTotal = Request[IO](Method.GET, uri("/calc-total/100.50"))

def getResult(req: Request[IO]): IO[String] =
  userService.orNotFound(req).flatMap(EntityDecoder.decodeString[IO])

println(getResult(getVAT).unsafeRunSync)
println(getResult(getTotal).unsafeRunSync)
```