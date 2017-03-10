---
layout: docs
title: A common stack
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
capabilities to fail, to read from our `Config` configuration and to accumulate some logging messages using respectively the `error`, the `reader` and  the `writer` effect.

```tut:book
import freestyle.effects.error._
import freestyle.effects.{reader, writer}

object modules {
  val rd = reader[Config]
  val wr = writer[Vector[String]]

  @module trait Persistence[F[_]] {
    val customer: algebras.CustomerPersistence[F]
    val stock: algebras.StockPersistence[F]
  }

  @module trait App[F[_]] {
    val persistence: Persistence[F]

    val errorM: ErrorM[F]
    val readerM: rd.ReaderM[F]
    val writerM: wr.WriterM[F]
  }
}
```

To validate the order we check if the number of crates that is ordered bigger is than zero and that the requested apple cultivar is selled by the company.

For the second part we use the [`reader`](/docs/effects/#error) effect algebra to read the set of apple cultivars from our `Config`.

We are here using [`Validated`](http://typelevel.org/cats/api/cats/data/Validated.html) to combine multiple errors in a [`NonEmptyList`](http://typelevel.org/cats/api/cats/data/NonEmptyList.html) represented by the type alias `cats.data.ValidatedNel`. `Validated` provides an easy way to accumate errors and more info can be found on the [`Cats` website](http://typelevel.org/cats/datatypes/validated.html). `NonEmptyList` is like a `List` with at least one element.

The `|+|` syntax used below is provided by Cats (because `Validated` has a [`Semigroup`](http://typelevel.org/cats/typeclasses/semigroup.html) instance) which makes it possible to combine the error messages of the two checks.

```tut:book
import cats.implicits._

import modules._
import cats.data.{NonEmptyList, ValidatedNel}

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

We will create a couple of domain specific exception case classes.

```tut:silent
sealed abstract class AppleException(val message: String) extends Exception(message)
case class CustomerNotFound(id: CustomerId)               extends AppleException(s"Customer $id can not be found")
case class QuantityNotAvailable(error: String)            extends AppleException(error)
case class ValidationFailed(errors: NonEmptyList[String]) extends AppleException(errors.intercalate("\n"))
```

We can use the `validateOrder` method in combination with our persistence algebras and the [`error`](/docs/effects/#error)
 and [`writer`](/docs/effects/#writer)) effect algebras, to create the `processOrder` method tying everything together.

The `error` effect makes it possible to fail a computation with an error, while the `writer` effect allows us to accumulate log messages in this case.

```tut:book
import algebras._

def processOrder[F[_]](order: Order)(implicit app: App[F]): FreeS[F, String] = {
  import app.persistence._, app.errorM._, app.writerM._
  for {
   customerOpt <- customer.getCustomer(order.customerId)
   customer    <- either(customerOpt.toRight(CustomerNotFound(order.customerId)))
   _           <- tell(Vector(s"customer name is ${customer.name}"))
   validation  <- validateOrder[F](order, customer)
   _           <- either(validation.toEither.leftMap(ValidationFailed))
   nbAvailable <- stock.checkQuantityAvailable(order.cultivar)
   _           <- tell(Vector(s"availble crates of ${order.cultivar} apples"))
   _           <- either(
                    Either.cond(
                      order.crates <= nbAvailable,
                      (),
                      QuantityNotAvailable(
                        s"""There are not sufficient crates of ${order.cultivar} apples in stock
                           |(only $nbAvailable available, while ${order.crates} needed).""".stripMargin)
                    )
                  )
   _          <- stock.registerOrder(order)
  } yield s"Order registered for customer ${order.customerId}"
}
```

As target type of our computation we choose a combination of `Kleisli` (also known as `ReaderT`), `WriterT` and `fs2.Task`.

- [`Kleisli`](http://typelevel.org/cats/datatypes/kleisli.html) or `ReaderT` is like an `A => F[B]` function, where `A` can be seen as an [environment or configuration](http://typelevel.org/cats/datatypes/kleisli.html#configuration) type.
- `WriterT` allows it to accumulate something along with passing on the actual result, in our case logging messages.
- [`fs2.Task`](https://oss.sonatype.org/service/local/repositories/releases/archive/co/fs2/fs2-core_2.12/0.9.4/fs2-core_2.12-0.9.4-javadoc.jar/!/fs2/Task.html) is a data type that can be used for potentially lazy computations which can contain asynchronous steps. The `Task` implementation used in this case is from the [FS2 library](https://github.com/functional-streams-for-scala/fs2), similar data types can be found in [Monix](https://github.com/monix/monix) ([`Monix.eval.Task`](https://monix.io/docs/2x/eval/task.html)) and [Scalaz](https://github.com/scalaz/scalaz) ([`scalaz.concurrent.Task`](http://timperrett.com/2014/07/20/scalaz-task-the-missing-documentation/)).

We use `Kleisli` to fullfill the `ReaderM` contstraint, `WriterT` for `WriterM` and `Task` for `ErrorM`.

```tut:book
import cats.data.{Kleisli, WriterT}
import _root_.fs2.Task
import _root_.fs2.interop.cats._

type Stack[A] = Kleisli[WriterT[Task, Vector[String], ?], Config, A]
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
      customers.get(id).pure[Stack]
  }

implicit val stockPersistencteHandler: StockPersistence.Handler[Stack] =
  new StockPersistence.Handler[Stack] {
    def checkQuantityAvailable(cultivar: String): Stack[Int] =
      (cultivar.toLowerCase match {
        case "granny smith" => 150
        case "jonagold"     => 200
        case _              => 25
      }).pure[Stack]


    def registerOrder(order: Order): Stack[Unit] =
      Kleisli.lift(WriterT.lift(Task.delay(println(s"Register $order"))))
  }
```

Below are some type class instances for `Kleisli` which are currently not yet provided by Cats.

```tut:book
import cats.data.Kleisli

// MonadError for Kleisli is on its way already
// https://github.com/typelevel/cats/pull/1543

import cats.{Monad, MonadError, MonadWriter}

trait KleisliMonad[F[_], A] {
  implicit val F: Monad[F]

  def pure[B](x: B): Kleisli[F, A, B] =
    Kleisli.pure[F, A, B](x)

  def flatMap[B, C](fa: Kleisli[F, A, B])(f: B => Kleisli[F, A, C]): Kleisli[F, A, C] =
    fa.flatMap(f)

  def tailRecM[B, C](b: B)(f: B => Kleisli[F, A, Either[B, C]]): Kleisli[F, A, C] =
    Kleisli[F, A, C]({ a => F.tailRecM(b) { f(_).run(a) } })
}

trait KleisliMTL1 {
  implicit def kleisliMonadError[F[_], A, E](implicit ME: MonadError[F, E]): MonadError[Kleisli[F, A, ?], E] = 
    new MonadError[Kleisli[F, A, ?], E] with KleisliMonad[F, A] {
      implicit val F = ME

      def raiseError[B](e: E): Kleisli[F, A, B] = Kleisli(_ => ME.raiseError(e))

      def handleErrorWith[B](kb: Kleisli[F, A, B])(f: E => Kleisli[F, A, B]): Kleisli[F, A, B] = Kleisli { a: A =>
        ME.handleErrorWith(kb.run(a))((e: E) => f(e).run(a))
      }
    }
}

object KleisliMTL extends KleisliMTL1 {
  implicit def kleisliMonadWriter[F[_], A, L](implicit MW: MonadWriter[F, L]): MonadWriter[Kleisli[F, A, ?], L] = 
    new MonadWriter[Kleisli[F, A, ?], L] with KleisliMonad[F, A] {
      implicit val F = MW

      def writer[B](bw: (L, B)): Kleisli[F, A, B] =
        Kleisli.lift(MW.writer(bw))

      def listen[B](fa: Kleisli[F, A, B]): Kleisli[F, A, (L, B)] =
        Kleisli(a => MW.listen(fa.run(a)))

      def pass[B](fb: Kleisli[F, A, (L => L, B)]): Kleisli[F, A, B] =
        Kleisli(a => MW.pass(fb.run(a)))

      override def tell(l: L): Kleisli[F, A, Unit] = 
        Kleisli.lift(MW.tell(l))
    }
}
```

We can now create a Freestyle program by specifying the type in `processOrder` as `App.Op`.

```tut:book
val program: FreeS[App.Op, String] =
  processOrder[App.Op](Order(50, "granny smith", customerId1))
```

With the persistence algebras handlers in place and with the right imports for the Freestyle effect algebras, we can execute to program resulting in `Stack[String]`.

```tut:book
import freestyle.effects.error.implicits._
import wr.implicits._
import rd.implicits._
import KleisliMTL._

val result: Stack[String] = program.exec[Stack]
```

We can run our `Stack` by supplying a `Config` value, running the `WriterT` and running the `Task`.

Supplying the `Config` with `result.run(config)` turns our `Stack[String]` in a `WriterT[Task, Vector[String], String]`, which is turned into a `Task[(Vector[String], String)]` with `run`.

```tut:book
val cultivars = Set("granny smith", "jonagold", "boskoop")
val config = Config(cultivars)

val task: Task[(Vector[String], String)] = result.run(config).run

task.unsafeRunSync.fold(println, println)
```
