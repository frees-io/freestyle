---
layout: docs
title: A common stack
permalink: /docs/stack/
---


```tut:book
import freestyle._
import freestyle.implicits._

import cats.implicits._

import java.util.UUID

type CustomerId = UUID

case class Customer(id: CustomerId, name: String)
case class Order(crates: Int, cultivar: String, customerId: CustomerId)

case class Config(cultivars: Set[String])

object algebras {
  @free trait CustomerPersistence[F[_]] {
    def getCustomer(id: CustomerId): FreeS.Par[F, Option[Customer]]
  }

  @free trait StockPersistence[F[_]] {
    def checkQuantityAvailable(cultivar: String): FreeS.Par[F, Int]
    def registerOrder(order: Order): FreeS.Par[F, Unit]
  } 
}

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
    val readerM: rd.ReaderM[F]
    val cacheM: cacheP.CacheM[F]
  }
}


 
import modules._
import cats.data.{NonEmptyList, ValidatedNel}

// import freestyle.effects.reader
// val rd = reader[Config]

import rd.implicits._

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




import cats.data.OptionT
// import freestyle.cache.KeyValueProvider

/// val cacheP = new KeyValueProvider[CustomerId, Customer]
// import cacheP.implicits._

import cacheP.implicits._


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




// import freestyle.effects.error._

sealed abstract class AppleException(val message: String) extends Exception(message)
case class CustomerNotFound(id: CustomerId)               extends AppleException(s"Customer $id can not be found")
case class QuantityNotAvailable(error: String)            extends AppleException(error)
case class ValidationFailed(errors: NonEmptyList[String]) extends AppleException(errors.intercalate("\n"))


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





import freestyle.effects.error.implicits._


val program: FreeS[App.T, String] =
  processOrder[App.T](Order(50, "granny smith", customerId1))



val cultivars = Set("granny smith", "jonagold", "boskoop")
val config = Config(cultivars)

val task: Task[String] = program.exec[Stack].run(config)

task.unsafeRunSync.fold(println, println)
```


