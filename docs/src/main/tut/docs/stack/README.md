---
layout: docs
title: A common stack with cache
permalink: /docs/stack/
---

In the following example we will see how a module with two algebras can be used in combination with some of the effect algebras provided by Freestyle.

In this example a company selling multiple types (cultivars) of apples wants to process the orders of its customers. The order needs to be validated,
we need to check if the requested amount of apples is in stock and in the end register the order.

We start with the imports of the Freestyle core.

```tut:silent
import freestyle._
import freestyle.implicits._
```

Now we can create some data types for a customer and an order together with a `Config` data type which holds a set of all the apple cultivars the company
sells.

```tut:silent
import java.util.UUID

type CustomerId = UUID

case class Customer(id: CustomerId, name: String)
case class Order(crates: Int, cultivar: String, customerId: CustomerId)

case class Config(cultivars: Set[String])
```

It should be possible to get a `Customer` for a specified `CustomerId` from some data store holding the company's customers.
We should be able to check how much crates of some specific apple cultivar the company has in stock and finally to register an order.

We will represent these capabilities using two algebras: `CustomerPersistence` and `StockPersistence`.

```tut:book
object algebras {
  @free trait CustomerPersistence[F[_]] {
    def getCustomer(id: CustomerId): FreeS.Par[F, Option[Customer]]
  }

  @free trait StockPersistence[F[_]] {
    def checkQuantityAvailable(cultivar: String): FreeS.Par[F, Int]
    def registerOrder(order: Order): FreeS.Par[F, Unit]
  }
}
```

These two algebras will be combined together in a `Persistence` module, that in its turn will be a part of our `App` module, that additionally includes the
capabilities to fail, to read from our `Config` configuration and to cache `Customer`s using respectively `error` and `reader` from the _freestyle-effects_ module and `cache` from the _freestyle-cache_ module.

```tut:book
import freestyle.effects.error._
import freestyle.effects.reader
import freestyle.cache.KeyValueProvider

object modules {
  val rd = reader[Config]
  val cacheP = new KeyValueProvider[CustomerId, Customer]

  @module trait Persistence[F[_]] {
    val customer: algebras.CustomerPersistence[F]
    val stock: algebras.StockPersistence[F]
  }

  @module trait App[F[_]] {
    val persistence: Persistence[F]

    val errorM: ErrorM[F]
    val cacheM: cacheP.CacheM[F]
    val readerM: rd.ReaderM[F]
  }
}
```

To validate the order we check if the number of crates that is ordered bigger is than zero and that the requested apple cultivar is selled by the company.

For the second part we use the [`reader`](/docs/effects/#error) effect algebra to read the set of apple cultivars from our `Config`.

We are here using [`Validated`](http://typelevel.org/cats/api/cats/data/Validated.html) to combine multiple errors in a [`NonEmptyList`](http://typelevel.org/cats/api/cats/data/NonEmptyList.html) represented by the type alias `cats.data.ValidatedNel`. `Validated` provides an easy way to accumate errors and more info can be found on the [`Cats` website](http://typelevel.org/cats/datatypes/validated.html). `NonEmptyList` is like a `List` with at least one element.

The `|+|` syntax used below is provided by Cats (because `Validated` has a [`Semigroup`](http://typelevel.org/cats/typeclasses/semigroup.html) instance) which makes it possible to combine the error messages of the two checks.

```tut:book
import modules._
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits._

def validateOrder[F[_]](order: Order, customer: Customer)(implicit app: App[F]): FreeS.Par[F, ValidatedNel[String, Unit]] =
  app.readerM.reader { config =>
    val v = ().validNel[String]
    v.ensure(NonEmptyList.of(
      "Number of crates ordered should be bigger than zero"))(
      _ => order.crates > 0) |+|
    v.ensure(NonEmptyList.of(
      "Apple cultivar is not available"))(
      _ => config.cultivars.contains(order.cultivar.toLowerCase))
  }
```

When validating an order we need to get the information of the customer, if a customer places multiple orders we don't want to send a database request every time, so we can use the [`freestyle-cache`](/docs/effects/Cache) module to cache customers.

In the `getCustomer` function we try to find the customer in the cache using the cache effect algebra, otherwise falling back to getting the customer from a database (or other persistence store).

We are using the [`OptionT`](http://typelevel.org/cats/datatypes/optiont.html) monad transformer here. With Freestyle you generally don't have to deal with monad transformers in your actual domain code. They are only used in the handlers and when executing your program on the edge. In this case though Freestyle and the `OptionT` monad transformer work flawlessly together. `OptionT` is used a convenient wrapper over a `FreeS[F, Option[A]]` value and the `OptionT#orElseF` method makes it is easy to specify what needs to happen if the inner `Option` is `None`.

```tut:book
import cats.data.OptionT

def getCustomer[F[_]](id: CustomerId)(implicit app: App[F]): FreeS[F, Option[Customer]] =
  // first try to get the customer from the cache
  OptionT(app.cacheM.get(id).freeS).orElseF {
    // otherwise fallback and get the customer from a persistent storehttp://typelevel.org/cats/datatypes/optiont.html
    for {
      customer <- app.persistence.customer.getCustomer(id).freeS
      _        <- customer.fold(
                    ().pure[FreeS[F, ?]])(
                    // put customer in cache
                    cust => app.cacheM.put(id, cust))
    } yield customer
  }.value
```

Next we create a couple of domain specific exception case classes.

```tut:silent
sealed abstract class AppleException(val message: String) extends Exception(message)
case class CustomerNotFound(id: CustomerId)               extends AppleException(s"Customer $id can not be found")
case class QuantityNotAvailable(error: String)            extends AppleException(error)
case class ValidationFailed(errors: NonEmptyList[String]) extends AppleException(errors.intercalate("\n"))
```

We can use the `validateOrder` and `getCustomer` methods in combination with our persistence algebras and the `error` effect algebra, to create the `processOrder` method tying everything together.

```tut:book
def processOrder[F[_]](order: Order)(implicit app: App[F]): FreeS[F, String] = {
  import app.persistence._, app.errorM._
  for {
    customerOpt <- getCustomer[F](order.customerId)
    customer    <- either(customerOpt.toRight(CustomerNotFound(order.customerId)))
    validation  <- validateOrder[F](order, customer)
    _           <- either(validation.toEither.leftMap(ValidationFailed))
    nbAvailable <- stock.checkQuantityAvailable(order.cultivar)
    _           <- either(
                     Either.cond(
                       order.crates <= nbAvailable,
                       (),
                       QuantityNotAvailable(
                         s"""There are not sufficient crates of ${order.cultivar} apples in stock
                            |(only $nbAvailable available, while ${order.crates} needed - $order).""".stripMargin)
                     )
                   )
    _          <- stock.registerOrder(order)
  } yield s"Order registered for customer ${order.customerId}"
}
```

As target type of our computation we choose a combination of `Kleisli` and `fs2.Task`.

- [`Kleisli`](http://typelevel.org/cats/datatypes/kleisli.html) or `ReaderT` is like an `A => F[B]` function, where `A` can be seen as an [environment or configuration](http://typelevel.org/cats/datatypes/kleisli.html#configuration) type. In this case `Stack` can be seen as `Config => Task[A]`,
- [`fs2.Task`](https://oss.sonatype.org/service/local/repositories/releases/archive/co/fs2/fs2-core_2.12/0.9.4/fs2-core_2.12-0.9.4-javadoc.jar/!/fs2/Task.html) is a data type that can be used for potentially lazy computations which can contain asynchronous steps. The `Task` implementation used in this case is from the [FS2 library](https://github.com/functional-streams-for-scala/fs2), similar data types can be found in [Monix](https://github.com/monix/monix) ([`Monix.eval.Task`](https://monix.io/docs/2x/eval/task.html)) and [Scalaz](https://github.com/scalaz/scalaz) ([`scalaz.concurrent.Task`](http://timperrett.com/2014/07/20/scalaz-task-the-missing-documentation/)).

We use `Kleisli` to fullfill the `ReaderM` constraint and `Task` the `ErrorM` constraint.


```tut:book
import cats.data.Kleisli
import _root_.fs2.Task
import _root_.fs2.interop.cats._

type Stack[A] = Kleisli[Task, Config, A]
```

Now we create the interpreters or handlers for algebras of our `Persistence` module by implementing their specific `Handler` traits as `x.Handler[Stack]`.

```tut:book
import algebras._

val customerId1 = UUID.fromString("00000000-0000-0000-0000-000000000000")

implicit val customerPersistencteHandler: CustomerPersistence.Handler[Stack] =
  new CustomerPersistence.Handler[Stack] {
    val customers: Map[CustomerId, Customer] =
      Map(customerId1 -> Customer(customerId1, "Apple Juice Ltd"))
    def getCustomer(id: CustomerId): Stack[Option[Customer]] =
      Kleisli(_ => Task.now(customers.get(id)))
  }

implicit val stockPersistencteHandler: StockPersistence.Handler[Stack] =
  new StockPersistence.Handler[Stack] {
    def checkQuantityAvailable(cultivar: String): Stack[Int] =
      Kleisli(_ =>
        Task.now(cultivar.toLowerCase match {
          case "granny smith" => 150
          case "jonagold"     => 200
          case _              => 25
        })
      )

    def registerOrder(order: Order): Stack[Unit] =
      Kleisli(_ => Task.delay(println(s"Register $order")))
  }
```

A handler that can cache `Customer`s can be created with a `ConcurrentHashMapWrapper` and a natural transformation to our `Stack` type.

```tut:book
import cats.{~>, Id}
import freestyle.cache.hashmap._
import freestyle.cache.KeyValueMap

implicit val freestyleHasherCustomerId: Hasher[CustomerId] =
  Hasher[CustomerId](_.hashCode)

implicit val cacheHandler: cacheP.CacheM.Handler[Stack] = {
  val rawMap: KeyValueMap[Id, CustomerId, Customer] =
    new ConcurrentHashMapWrapper[Id, CustomerId, Customer]

  val cacheIdToStack: Id ~> Stack =
    new (Id ~> Stack) {
      def apply[A](a: Id[A]): Stack[A] = a.pure[Stack]
    }

  cacheP.implicits.cacheHandler(rawMap, cacheIdToStack)
}
```

We can now create a Freestyle program by specifying the type in `processOrder` as `App.Op`.

```tut:book
val program: FreeS[App.Op, String] =
  processOrder[App.Op](Order(50, "granny smith", customerId1))
```

With the persistence algebras interpreters and the `Customer` cache interpreter in place and with the right imports for the `reader` and `error` effect, we can execute to program resulting in `Stack[String]`.

```tut:book
import freestyle.effects.error.implicits._
import rd.implicits._

val cultivars = Set("granny smith", "jonagold", "boskoop")
val config = Config(cultivars)

val result: Stack[String] = program.exec[Stack]
```

We can run our `Stack` by supplying a `Config` value and running the `Task`.

By supplying a `Config` value we can run our `Stack` which results in a `Task[String]`.
When we want to actually execute the program, we can run the `Task` by using one of the methods on `Task`.

```tut:book
val task: Task[String] = result.run(config)

task.unsafeRunSync.fold(println, println)
```

