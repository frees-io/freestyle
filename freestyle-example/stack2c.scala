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

    val errorM: ErrorM[F]
    // val readerM: rd.ReaderM[F]
    val writerM: wr.WriterM[F]
    val readerM: rd.ReaderM[F]
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
  import _root_.fs2.interop.cats._
  // import _root_.fs2.interop.cats.{uf1ToFunctionK => _, _}

  type Stack[A] = Kleisli[WriterT[Task, Vector[String], ?], Config, A]


  // algebra interpreters

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
        Kleisli.lift(WriterT.lift(Task.delay(println(s"Register $order"))))
    }


  // provide MTL instances for kleisli
  import cats.{Monad, MonadError, MonadWriter}


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

  private trait KleisliMonad[F[_], A] {
    implicit val F: Monad[F]

    def pure[B](x: B): Kleisli[F, A, B] =
      Kleisli.pure[F, A, B](x)

    def flatMap[B, C](fa: Kleisli[F, A, B])(f: B => Kleisli[F, A, C]): Kleisli[F, A, C] =
      fa.flatMap(f)

    def tailRecM[B, C](b: B)(f: B => Kleisli[F, A, Either[B, C]]): Kleisli[F, A, C] =
      Kleisli[F, A, C]({ a => F.tailRecM(b) { f(_).run(a) } })
  }

  

  import KleisliMTL._


  // check instances for provider effects are here

  import cats.MonadReader
  val monadStack  = Monad[Stack]
  val mw = MonadWriter[Stack, Vector[String]]
  val mr = MonadReader[Stack, Config]
  val me = MonadError[Stack, Throwable]


  // create program

  import freestyle.effects.error.implicits._
  import wr.implicits._
  import rd.implicits._

  val program: FreeS[App.T, String] =
    processOrder[App.T](Order(50, "granny smith", customerId1))

  
  // check if we can create frees programs using App module

  val app = App[App.T]
  def g[A]: (Config => A) => FreeS[App.T, A] = f => app.readerM.reader(f)
  val cfgInt: FreeS[App.T, Int]  = app.readerM.reader(_.cultivars.size)
  val error:  FreeS[App.T, Int]  = app.errorM.catchNonFatal(cats.Eval.later(1 + 2))
  val write:  FreeS[App.T, Unit] = app.writerM.tell(Vector("hello foo"))


  // manual coproduct

  import cats.data.Coproduct
  type CP1[α] = Coproduct[ErrorM.T, Persistence.T, α]
  type CP2[α] = Coproduct[wr.WriterM.T, CP1, α]
  type AppCP[α] = Coproduct[rd.ReaderM.T, CP2, α]

  val write2:  FreeS[AppCP, Unit] = app.writerM.tell(Vector("hello foo")).freeS

  val appEq = implicitly[App.T[α] =:= AppCP[α] forSome { type α }]


  // check if interpreters can be resolved

  val interp1: Persistence.T ~> Stack = implicitly[Persistence.T ~> Stack]
  val interp2: rd.ReaderM.T  ~> Stack = implicitly[rd.ReaderM.T  ~> Stack]
  val interp3: wr.WriterM.T  ~> Stack = implicitly[wr.WriterM.T  ~> Stack]
  val interp4: ErrorM.T      ~> Stack = implicitly[ErrorM.T      ~> Stack]

  // val interp0: App.T         ~> Stack = implicitly[App.T         ~> Stack]


  // diverging implicit expansion for type freestyle.example.program3.CP2 ~> freestyle.example.program3.Stack
  //  starting with method freeStyleErrorMInterpreter in object implicits
  //    val inC2:   CP2   ~> Stack = implicitly[CP2 ~> Stack]
  //
  // diverging implicit expansion for type freestyle.example.program3.AppCP ~> freestyle.example.program3.Stack
  // starting with method freestyleWriterMInterpreter in object implicits
  //   val inCApp: AppCP ~> Stack = implicitly[AppCP ~> Stack]
  //
  val inC1:   CP1   ~> Stack = implicitly[CP1 ~> Stack]
  // val inC2:   CP2   ~> Stack = implicitly[CP2 ~> Stack]
  // val inCApp: AppCP ~> Stack = implicitly[AppCP ~> Stack]


  type ReaderWriterCP[α] = Coproduct[rd.ReaderM.T, wr.WriterM.T, α]
  val interpReaderWriterCP: ReaderWriterCP ~> Stack = implicitly[ReaderWriterCP ~> Stack]

  type WriterReaderCP[α] = Coproduct[wr.WriterM.T, rd.ReaderM.T, α]
  val interpWriterReaderCP: WriterReaderCP ~> Stack = implicitly[WriterReaderCP ~> Stack]

  type ErrorReaderWriterCP[α] = Coproduct[ErrorM.T, ReaderWriterCP, α]
  // val interpErrorReaderWriterCP: ErrorReaderWriterCP ~> Stack =
  //   implicitly[ErrorReaderWriterCP ~> Stack]
  val interpErrorReaderWriterCPmanual: ErrorReaderWriterCP ~> Stack = 
    interpretCoproduct(interp4, interpretCoproduct[rd.ReaderM.T, wr.WriterM.T, Stack])
  // val interpErrorReaderWriterCPsemi: ErrorReaderWriterCP ~> Stack = 
  //   interpretCoproduct[ErrorM.T, ReaderWriterCP, Stack]


  val appInterpreter: App.T ~> Stack =
    interpretCoproduct(interp2, interpretCoproduct(interp3, interpretCoproduct[ErrorM.T, Persistence.T, Stack]))

  // run program

  val cultivars = Set("granny smith", "jonagold", "boskoop")
  val config = Config(cultivars)

  // val task: Task[(Vector[String], String)] = program.exec[Stack].run(config).run
  val task: Task[(Vector[String], String)] =
    program.exec[Stack](
      monadStack,
      interpretAp(monadStack, appInterpreter)
    ).run(config).run

  task.unsafeRunSync.fold(println, println)

}