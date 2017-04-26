---
layout: docs
title: http4s
permalink: /docs/integrations/http4s/
---

# http4s

A Freestyle program can easily be used with [http4s](http://http4s.org/).

You can add the _freestyle-http4s_ module as follows:

[comment]: # (Start Replace)

```scala
libraryDependencies += "com.47deg" %% "freestyle-http-http4s" % "0.1.0"
```

[comment]: # (End Replace)

Note that Freestyle only supports the http4s version based on Cats and FS2.

## Integration

The _freestyle-http4s_ module allows you to return a `FreeS[F, A]` value if there is an `F.Handler[G]` in scope and an `EntityEncoder[G[A]]`. Http4s provides `EntityEncoder` instances for `fs2.Task`, `Future`, `Id`, etc out of the box if there is an `EntityEncoder[A]` in scope.

## Example

First, lets import all the regular freestyle imports:

```tut:silent
import freestyle._
import freestyle.implicits._
```

And the specific import for `freestyle-http4s`:

```tut:silent
import freestyle.http.http4s._
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
import _root_.fs2.Task
import _root_.fs2.interop.cats._

implicit val taskHandler = new CalcVAT.Handler[Task] {
  private val rate: BigDecimal = BigDecimal(20) / BigDecimal(100)
  def vat(price: BigDecimal): Task[BigDecimal] =
    Task.now(price * rate)
}
```

Some imports for http4s:

```tut:silent
import org.http4s._
import org.http4s.dsl._
```

We can now create a simple `HttpService`:

```tut:book
object Price {
  def unapply(s: String): Option[BigDecimal] =
    if (s.isEmpty) None else Some(BigDecimal(s))
}

val userService = HttpService {
  case GET -> Root / "calc-vat" / Price(p) =>
    Ok(CalcVAT[CalcVAT.Op].vat(p).map(vat => s"The VAT for $p is $vat"))
  case GET -> Root / "calc-total" / Price(p) =>
    Ok(CalcVAT[CalcVAT.Op].withVat(p).map(total => s"The total price including VAT is $total"))
}
```

Trying out our `HttpService`:

```tut:book
val getVAT = Request(Method.GET, uri("/calc-vat/100.50"))
val getTotal = Request(Method.GET, uri("/calc-total/100.50"))

def getResult(req: Request): Task[String] =
  userService.orNotFound(req).flatMap(EntityDecoder.decodeString)

getResult(getVAT).unsafeRunSync.fold(println, println)
getResult(getTotal).unsafeRunSync.fold(println, println)
```
