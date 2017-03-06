package freestyle.example

import freestyle._
import freestyle.implicits._

import cats.implicits._

import java.util.UUID

import types._

import freestyle.effects.error._
import freestyle.effects.reader
import freestyle.cache.KeyValueProvider

object modules2 {
  val rd = reader[Config]
  val cacheP = new KeyValueProvider[CustomerId, Customer]

  @module trait Persistence[F[_]] {
    val customer: algebras.CustomerPersistence[F]
    val stock: algebras.StockPersistence[F]
  }

  @module trait App[F[_]] {
    val persistence: Persistence[F]


    // diverging implicit expansion for typemodules2.App.T ~> program2.Stack
    // starting with method freestyleEffectsReaderInterpreter in object implicits
    //   val interp0: App.T ~> Stack = implicitly[App.T ~> Stack]
    //
    // diverging implicit expansion for type FunctionK[modules2.rd.ReaderM.T, program2.Stack]
    // starting with method freestyleEffectsReaderInterpreter in object implicits
    //   val task: Task[String] = program.exec[Stack].run(config)
    // val errorM: ErrorM[F]
    // val readerM: rd.ReaderM[F]
    // val cacheM: cacheP.CacheM[F]


    // diverging implicit expansion for type modules2.App.T ~> program2.Stack
    // starting with value cacheInterpreter in object program2
    //   val interp0: App.T ~> Stack = implicitly[App.T ~> Stack]
    //
    // diverging implicit expansion for type FunctionK[modules2.cacheP.CacheM.T,program2.Stack]
    // starting with value cacheInterpreter in object program2
    //   val task: Task[String] = program.exec[Stack].run(config)

    val errorM: ErrorM[F]
    val cacheM: cacheP.CacheM[F]
    val readerM: rd.ReaderM[F]
  }
}


object program2 {


  import modules2._
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
  // import _root_.fs2.interop.cats._
  import _root_.fs2.interop.cats.{uf1ToFunctionK => _, _}



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


  // implicit val cacheInterpreter: cacheP.CacheM.Interpreter[Stack] = {
  implicit val cacheInterpreter: cacheP.CacheM.T ~> Stack = {
    val rawMap: KeyValueMap[Id, CustomerId, Customer] =
      new ConcurrentHashMapWrapper[Id, CustomerId, Customer]

    val cacheIdToStack: Id ~> Stack =
      new (Id ~> Stack) {
        def apply[A](a: Id[A]): Stack[A] = a.pure[Stack]
      }

    cacheP.implicits.cacheInterpreter(rawMap, cacheIdToStack)
  }



  import freestyle.effects.error.implicits._
  import rd.implicits._

  val program: FreeS[App.T, String] =
    processOrder[App.T](Order(50, "granny smith", customerId1))



  val cultivars = Set("granny smith", "jonagold", "boskoop")
  val config = Config(cultivars)

  val app = App[App.T]
  def g[A]: (Config => A) => FreeS[App.T, A] = f => app.readerM.reader(f)
  val cfgInt: FreeS[App.T, Int] = app.readerM.reader(_.cultivars.size)
  val error: FreeS[App.T, Int] = app.errorM.catchNonFatal(cats.Eval.later(1 + 2))
  // val cachedthing: FreeS[App.T, Option[Customer]] = app.cacheM.get(customerId1)


  val interpRD: rd.ReaderM.T ~> Stack = implicitly[rd.ReaderM.T ~> Stack]

  val interp0: App.T ~> Stack = implicitly[App.T ~> Stack]
  val interp1: Persistence.T ~> Stack = implicitly[Persistence.T ~> Stack]
  // val interp2: cacheP.CacheM.T ~> Stack = implicitly[cacheP.CacheM.T ~> Stack]
  // val interp2: cacheP.CacheM.T ~> Stack = implicitly[cacheP.CacheM.Interpreter[Stack]](cacheInterpreter)
  // val interp2a: cacheP.CacheM.T ~> Stack = cacheInterpreter

  val task: Task[String] = program.exec[Stack].run(config)

  task.unsafeRunSync.fold(println, println)

}