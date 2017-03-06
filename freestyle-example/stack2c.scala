package freestyle.example

import freestyle._
import freestyle.implicits._

import cats.~>
import cats.implicits._

import java.util.UUID


import types._


import freestyle.effects.error._
import freestyle.effects.{reader, writer}

object modules3 {
  val rd = reader[Config]
  val wr = writer[Vector[String]]

  @module trait Persistence[F[_]] {
    val customer: algebras.CustomerPersistence[F]
    val stock: algebras.StockPersistence[F]
  }

  @module trait App[F[_]] {
    val persistence: Persistence[F]


    // stack2c.scala:141
    // diverging implicit expansion for type modules3.App.T ~> program3.Stack
    // starting with method freestyleEffectsReaderInterpreter in object implicits
    //   val interp0: App.T ~> Stack = implicitly[App.T ~> Stack]
    //
    // stack2c.scala:141
    // could not find implicit value for parameter e: modules3.rd.ReaderM.T ~> program3.Stack
    //   val interp2: rd.ReaderM.T ~> Stack = implicitly[rd.ReaderM.T ~> Stack]
    //
    // stack2c.scala:144
    // diverging implicit expansion for type cats.arrow.FunctionK[modules3.rd.ReaderM.T,program3.Stack]
    // starting with method freestyleEffectsReaderInterpreter in object implicits
    //   val task: Task[(Vector[String], String)] = program.exec[Stack].run.run(config)

    // val errorM: ErrorM[F]
    // val readerM: rd.ReaderM[F]
    // val writerM: wr.WriterM[F]


    // stack2c.scala:159
    // diverging implicit expansion for type modules3.App.T ~> program3.Stack
    // starting with method freestyleEffectsReaderInterpreter in object implicits
    //   val interp0: App.T ~> Stack = implicitly[App.T ~> Stack]
    //
    // stack2c.scala:161
    // could not find implicit value for parameter e: modules3.rd.ReaderM.T ~> program3.Stack
    //   val interp2: rd.ReaderM.T ~> Stack = implicitly[rd.ReaderM.T ~> Stack]
    //
    // stack2c.scala:164
    // diverging implicit expansion for type FunctionK[modules3.rd.ReaderM.T,program3.Stack]
    // starting with method freestyleEffectsReaderInterpreter in object implicits
    //   val task: Task[(Vector[String], String)] = program.exec[Stack].run.run(config)

    val errorM: ErrorM[F]
    val readerM: rd.ReaderM[F]
    val writerM: wr.WriterM[F]
  }
}


object program3 {


  import modules3._
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




  sealed abstract class AppleException(val message: String) extends Exception(message)
  case class CustomerNotFound(id: CustomerId)               extends AppleException(s"Customer $id can not be found")
  case class QuantityNotAvailable(error: String)            extends AppleException(error)
  case class ValidationFailed(errors: NonEmptyList[String]) extends AppleException(errors.intercalate("\n"))


  def processOrder[F[_]](order: Order)(implicit app: App[F]): FreeS[F, String] =
    for {
      customerOpt <- app.persistence.customer.getCustomer(order.customerId)
      customer    <- app.errorM.either(customerOpt.toRight(CustomerNotFound(order.customerId)))
      _           <- app.writerM.tell(Vector(s"customer name is ${customer.name}"))
      validation  <- validateOrder[F](order, customer)
      _           <- app.errorM.either(validation.toEither.leftMap(ValidationFailed))
      nbAvailable <- app.persistence.stock.checkQuantityAvailable(order.cultivar)
      _           <- app.writerM.tell(Vector(s"availble crates of ${order.cultivar} apples"))
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
  import cats.data.{Kleisli, WriterT}
  import _root_.fs2.Task
  // import _root_.fs2.interop.cats._
  import _root_.fs2.interop.cats.{uf1ToFunctionK => _, _}

  // type Stack[A] = Kleisli[Task, Config, A]
  type Stack[A] = WriterT[Kleisli[Task, Config, ?], Vector[String], A]


  val customerId1 = UUID.fromString("00000000-0000-0000-0000-000000000000")

  implicit val customerPersistencteInterpreter: CustomerPersistence.Interpreter[Stack] =
    new CustomerPersistence.Interpreter[Stack] {
      val customers: Map[CustomerId, Customer] =
        Map(customerId1 -> Customer(customerId1, "Apple Juice Ltd"))
      def getCustomerImpl(id: CustomerId): Stack[Option[Customer]] =
        customers.get(id).pure[Stack]
    }

  implicit val stockPersistencteInterpreter: StockPersistence.Interpreter[Stack] =
    new StockPersistence.Interpreter[Stack] {
      def checkQuantityAvailableImpl(cultivar: String): Stack[Int] =
        (cultivar.toLowerCase match {
          case "granny smith" => 150
          case "jonagold"     => 200
          case _              => 25
        }).pure[Stack]


      def registerOrderImpl(order: Order): Stack[Unit] =
        WriterT.lift(Kleisli(_ => Task.delay(println(s"Register $order"))))
    }


  import freestyle.effects.error.implicits._
  import wr.implicits._
  import rd.implicits._

  val program: FreeS[App.T, String] =
    processOrder[App.T](Order(50, "granny smith", customerId1))



  val cultivars = Set("granny smith", "jonagold", "boskoop")
  val config = Config(cultivars)

  val app = App[App.T]
  def g[A]: (Config => A) => FreeS[App.T, A] = f => app.readerM.reader(f)
  val cfgInt: FreeS[App.T, Int] = app.readerM.reader(_.cultivars.size)
  val error: FreeS[App.T, Int] = app.errorM.catchNonFatal(cats.Eval.later(1 + 2))
  val write: FreeS[App.T, Unit] = app.writerM.tell(Vector("hello foo"))

  val interp0: App.T ~> Stack = implicitly[App.T ~> Stack]
  val interp1: Persistence.T ~> Stack = implicitly[Persistence.T ~> Stack]
  val interp2: rd.ReaderM.T ~> Stack = implicitly[rd.ReaderM.T ~> Stack]
  val interp3: wr.WriterM.T ~> Stack = implicitly[wr.WriterM.T ~> Stack]

  val task: Task[(Vector[String], String)] = program.exec[Stack].run.run(config)

  task.unsafeRunSync.fold(println, println)

}