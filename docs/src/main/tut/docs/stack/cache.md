---
layout: docs
title: A common stack with cache
permalink: /docs/stack/cache/
---

In the following example we will see how a module with two algebras can be used in combination with some of the effect algebras provided by Freestyle.

In this example a company selling multiple types (cultivars) of apples wants to process the orders of its customers. The order needs to be validated,
we need to check if the requested amount of apples is in stock and in the end register the order.

We start with the imports of the Freestyle core.

```tut:book
import freestyle._
import freestyle.implicits._
```

Now we can create some data types for a customer and an order together with a `Config` data type which holds a set of all the apple cultivars the company
sells.

```tut:book
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

For the second part we use the `reader` effect algebra to read the set of apple cultivars from our `Config`.

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

When validating an order we need to get the information of the customer, if a customer places multiple orders we don't want to send a database request every time, so we could use the `freestyle-cache` module.

In the `getCustomer` function we try to find the customer in the cache, otherwise falling back to getting the customer from a database (or other persistence store).

```tut:book
import cats.data.OptionT

def getCustomer[F[_]](id: CustomerId)(implicit app: App[F]): FreeS[F, Option[Customer]] =
  // first try to get the customer from the cache
  OptionT(app.cacheM.get(id).freeS).orElseF {
    // otherwise fallback and get the customer from a persistent store
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

```tut:book
sealed abstract class AppleException(val message: String) extends Exception(message)
case class CustomerNotFound(id: CustomerId)               extends AppleException(s"Customer $id can not be found")
case class QuantityNotAvailable(error: String)            extends AppleException(error)
case class ValidationFailed(errors: NonEmptyList[String]) extends AppleException(errors.intercalate("\n"))
```

We can use the `validateOrder` and `getCustomer` methods in combination with our persistence algebras and the `error` effect algebra, to create the `processOrder` method tying everything together.

```tut:book
def processOrder[F[_]](order: Order)(implicit app: App[F]): FreeS[F, String] =
  for {
    customerOpt <- getCustomer[F](order.customerId)
    customer    <- app.errorM.either(customerOpt.toRight(CustomerNotFound(order.customerId)))
    validation  <- validateOrder[F](order, customer)
    _           <- app.errorM.either(validation.toEither.leftMap(ValidationFailed))
    nbAvailable <- app.persistence.stock.checkQuantityAvailable(order.cultivar)
    _           <- app.errorM.either(
                     Either.cond(
                       order.crates <= nbAvailable,
                       (),
                       QuantityNotAvailable(
                         s"""There are not sufficient crates of ${order.cultivar} apples in stock
                            |(only $nbAvailable available, while ${order.crates} needed - $order).""".stripMargin)
                     )
                   )
    _          <- app.persistence.stock.registerOrder(order)
  } yield s"Order registered for customer ${order.customerId}"
```

As target type of our computation we choose a combination of `Kleisli` and `fs2.Task`.

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

An interpreter which can cache the `Customer`s can be created with a `ConcurrentHashMapWrapper` and natural transformation to our `Stack` type.

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

```tut:book
val task: Task[String] = program.exec[Stack].run(config)

task.unsafeRunSync.fold(println, println)
```