---
layout: docs
title: A common stack
permalink: /docs/stack/
---

Story: A customer wants to place an order for some crates of apples.


```tut:book
import freestyle._
import freestyle.implicits._

import cats.implicits._

import java.util.UUID

type CustomerId = UUID

case class Customer(id: CustomerId, name: String)
case class Order(crates: Int, cultivar: String, customerId: CustomerId)
```


Some algebras to access information about customers, check our apple stock and register orders.

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

Group the algebras together in two modules.

```tut:book
object modules {
  @module trait Persistence[F[_]] {
    val customer: algebras.CustomerPersistence[F]
    val stock: algebras.StockPersistence[F]
  }

  @module trait App[F[_]] {
    val persistence: Persistence[F]
  }
}
```

We want to configure what types (cultivars) of apples we sell and we want to access this information when we validate an order, we can use the `reader` effect.

```tut:book
case class Config(cultivars: Set[String])

import modules._
import cats.data.{NonEmptyList, ValidatedNel}
import freestyle.effects.reader

val rd = reader[Config]
import rd.implicits._

def validateOrder[F[_]](order: Order, customer: Customer)(implicit RM: rd.ReaderM[F]): FreeS.Par[F, ValidatedNel[String, Unit]] =
  RM.reader { config =>
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
import freestyle.cache.KeyValueProvider

val cacheP = new KeyValueProvider[CustomerId, Customer]
import cacheP.implicits._

def getCustomer[F[_]: App: cacheP.CacheM](id: CustomerId): FreeS[F, Option[Customer]] =
  // first try to get the customer from the cache
  OptionT(cacheP.CacheM[F].get(id).freeS).orElseF {
    // otherwise fallback and get the customer from a persistent store
    for {
      customer <- App[F].persistence.customer.getCustomer(id).freeS
      _        <- customer.fold(
                    ().pure[FreeS[F, ?]])(
                    // put customer in cache
                    cust => cacheP.CacheM[F].put(id, cust))
    } yield customer
  }.value
```


We can now tie everything together in the `processOrder` function, which will get the customer, validate the order, check the stock and register the order. If one of these steps fail we want to short circuit the rest of the steps and return an error message by using the `error` effect,

```tut:book
import freestyle.effects.error._

sealed abstract class AppleException(val message: String) extends Exception(message)
case class CustomerNotFound(id: CustomerId)               extends AppleException(s"Customer $id can not be found")
case class QuantityNotAvailable(error: String)            extends AppleException(error)
case class ValidationFailed(errors: NonEmptyList[String]) extends AppleException(errors.intercalate("\n"))


def processOrder[F[_]: ErrorM: rd.ReaderM: cacheP.CacheM](order: Order)(implicit app: App[F]): FreeS[F, String] =
  for {
    customerOpt <- getCustomer[F](order.customerId)
    customer    <- ErrorM[F].either(customerOpt.toRight(CustomerNotFound(order.customerId)))
    validation  <- validateOrder[F](order, customer)
    _           <- ErrorM[F].either(validation.toEither.leftMap(ValidationFailed))
    nbAvailable <- app.persistence.stock.checkQuantityAvailable(order.cultivar)
    _           <- ErrorM[F].either(
                     Either.cond(
                       order.crates <= nbAvailable,
                       (),
                       QuantityNotAvailable(
                         s"""There are not sufficient crates of ${order.cultivar} apples in stock
                            |(${order.crates} needed, but only $nbAvailable available).""".stripMargin)
                     )
                   )
    _          <- app.persistence.stock.registerOrder(order)
  } yield s"Order registered for customer ${order.customerId}"
```

Now we would like to execute a program, so we need some interpreters.

We are using `Kleisli[Task, Config, ?]` as the target stack type.


```tut:book
import algebras._
import cats.data.Kleisli
import _root_.fs2.Task
import _root_.fs2.interop.cats._

type Stack[A] = Kleisli[Task, Config, A]

val customerId1 = UUID.fromString("00000000-0000-0000-0000-000000000000")

implicit val customerPersistencteInterpreter: CustomerPersistence.Interpreter[Stack] = 
  new CustomerPersistence.Interpreter[Stack] {
    val customers: Map[CustomerId, Customer] =      
      Map(customerId1 -> Customer(customerId1, "Apple Juice Ltd"))
    def getCustomerImpl(id: CustomerId): Stack[Option[Customer]] =
      Kleisli(_ => Task.now(customers.get(id)))
  }

implicit val stockPersistencteInterpreter: StockPersistence.Interpreter[Stack] = 
  new StockPersistence.Interpreter[Stack] {
    def checkQuantityAvailableImpl(cultivar: String): Stack[Int] =
      Kleisli(_ => 
        Task.now(cultivar.toLowerCase match {
          case "granny smith" => 150
          case "jonagold"     => 200
          case _              => 25
        })
      )

    def registerOrderImpl(order: Order): Stack[Unit] =
      Kleisli(_ => Task.delay(println(s"Register $order")))
  }
```

An interpreter for `cache` using a `ConcurrentHashMap` wrapper.

```tut:book
import cats.{~>, Id}
import freestyle.cache.hashmap._
import freestyle.cache.KeyValueMap

implicit val freestyleHasherCustomerId: Hasher[CustomerId] = 
  Hasher[CustomerId](_.hashCode)

val rawMap: KeyValueMap[Id, CustomerId, Customer] =
  new ConcurrentHashMapWrapper[Id, CustomerId, Customer]

val cacheIdToStack: Id ~> Stack =
  new (Id ~> Stack) {
    def apply[A](a: Id[A]): Stack[A] = a.pure[Stack]
  }

implicit val cacheInterpreter: cacheP.CacheM.Interpreter[Stack] =
  cacheP.implicits.cacheInterpreter(rawMap, cacheIdToStack)
```


Building a freestyle program:

```tut:book
import freestyle.effects.error.implicits._

import cats.data.Coproduct

type AppError[A]            = Coproduct[ErrorM.T, App.T, A]
type AppErrorReader[A]      = Coproduct[rd.ReaderM.T, AppError, A]
type AppErrorReaderCache[A] = Coproduct[cacheP.CacheM.T, AppErrorReader, A]

// val errorM = ErrorM[AppError]

val program: FreeS[App.T, String] =
  processOrder[App.T](Order(50, "granny smith", customerId1))

val program: FreeS[AppErrorReaderCache, String] =
  processOrder[AppErrorReaderCache](Order(50, "granny smith", customerId1))
```

Running the program:

```tut:book
val cultivars = Set("granny smith", "jonagold", "boskoop")
val config = Config(cultivars)

val task: Task[String] = program.exec[Stack].run(config)

task.unsafeRunSync.fold(println, println)
```

