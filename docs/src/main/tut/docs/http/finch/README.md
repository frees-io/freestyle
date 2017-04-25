---
layout: docs
title: http4s
permalink: /docs/integrations/finch/
---

# finch

A freestyle program can easily be used with [Finch](https://finagle.github.io/finch/).

You can add the _freestyle-finch_ module as follows:

[comment]: # (Start Replace)

```scala
libraryDependencies += "com.47deg" %% "freestyle-http-finch" % "0.1.0"
```

[comment]: # (End Replace)

## Integration

The _freestyle-finch_ module allows you to return a `FreeS[F, io.finch.Output[A]]` value if there is either a `F.Handler[Id]` or `F.Handler[Future]` in scope (where `Future` is Twitter's `Future`) within an `Endpoint#apply` (eg `get(string) { ... }`).

## Example

The standard freestyle imports:

```tut:silent
import freestyle._
import freestyle.implicits._
```

In this example, we will create a Finch `Endpoint` which will be able to calculate the greatest common divisor (GCD) of two natural numbers:

```tut:book
@free trait Calc {
  def gcd(a: Int, b: Int): FS[Int]
}
```

The greatest common divisor can be computed using Euclid's algorithm:

```tut:book
def gcd(a: Int, b: Int): Int =
  if (b == 0) a.abs else gcd(b, a % b)
```

A version returning a twitter `Future`:

```tut:book
import com.twitter.util.Future

def gcdFuture(a: Int, b: Int): Future[Int] =
  Future.value(gcd(a, b))
```

We can now create a `Calc.Handler[Future]`:

```tut:book
import io.catbird.util._

implicit val futureHandler = new Calc.Handler[Future] {
  def gcd(a: Int, b:Int): Future[Int] = gcdFuture(a, b)
}
```

Importing the _freestyle-finch_ module, Finch, and shapeless' `::` used by Finch:

```tut:silent
import freestyle.http.finch._

import io.finch._

import shapeless.::
```

We can use a freestyle program within `Endpoint#apply`:

```tut:book
cats.Monad[Future]

val gcdEndpointTwo = get(int :: int) { (a: Int, b: Int) =>
  Calc[Calc.Op].gcd(a, b).map(Ok(_))
}

val gcdEndpointOne = get(int) { (a: Int) =>
  Calc[Calc.Op].gcd(a, 12).map(Ok(_))
}

val gcdEndpointZero = get(/) {
  Calc[Calc.Op].gcd(2209, 94).map(Ok(_))
}
```

We can combine the three endpoints and check the responses if we access the three endpoints:

```tut:book
import shapeless.:+:

val endpoint = gcdEndpointTwo :+: gcdEndpointOne :+: gcdEndpointZero

endpoint(Input.get("/83/2671")).awaitValueUnsafe()
endpoint(Input.get("/30")).awaitValueUnsafe()
endpoint(Input.get("/")).awaitValueUnsafe()
```
