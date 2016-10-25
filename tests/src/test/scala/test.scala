

// object definitions {

//   @free sealed abstract class ServiceA[F[_]] {

//     def op1(op1_p1: Int): Free[F, List[Int]]

//     def op2(op2_p1: Int): Free[F, List[Int]]

//     def op3(op3_p1: Int, op3_p2: Int): Free[F, List[Int]] =
//       for {
//         a <- op1(1)
//         b <- op2(2)
//       } yield a ++ b
//   }

//   @free sealed trait ServiceB[F[_]] {

//     def op4(op4_p1: Int): Free[F, List[Int]]

//     def op5(op5_p1: Int): Free[F, List[Int]]

//     def op6(op6_p1: Int, op6_p2: Int): Free[F, List[Int]] =
//       for {
//         a <- op4(1)
//         b <- op5(2)
//       } yield a ++ b
//   }

//   @free trait ServiceC[F[_]] {

//     def op7(op7_p1: Int): Free[F, List[Int]]

//     def op8(op8_p1: Int): Free[F, List[Int]]

//     def op9(op9_p1: Int, op9_p2: Int): Free[F, List[Int]] =
//       for {
//         a <- op7(1)
//         b <- op8(2)
//       } yield a ++ b
//   }

//   @free trait ServiceD[F[_]] {

//     def op10(op10_p1: Int): Free[F, List[Int]]

//     def op11(op11_p1: Int): Free[F, List[Int]]

//   }

//   @free trait ServiceE[F[_]] {

//     def op12(op12_p1: Int): Free[F, List[Int]]

//     def op13(op13_p1: Int): Free[F, List[Int]]

//   }

//   @module trait Persistence[F[_]] {

//     val serviceA: ServiceA[F]

//     val serviceB: ServiceB[F]

//     val serviceC: ServiceC[F]

//   }

//   @module trait BizLogic[F[_]] {

//     val serviceD: ServiceD[F]

//     val servicee: ServiceE[F]

//   }

//   @module trait App[F[_]] {

//     val persistence: Persistence[F]

//     val bizLogic: BizLogic[F]

//   }

// }

// object runtimes {

//   import definitions._

//   implicit object ServiceAInterpreter extends ServiceA.Interpreter[Option] {

//     def op1Impl(op1_p1: Int): Option[List[Int]] = Option(op1_p1 :: Nil)

//     def op2Impl(op2_p1: Int): Option[List[Int]] = Option(op2_p1 :: Nil)

//   }

//   implicit object ServiceBInterpreter extends ServiceB.Interpreter[Option] {

//     def op4Impl(op4_p1: Int): Option[List[Int]] = Option(op4_p1 :: Nil)

//     def op5Impl(op5_p1: Int): Option[List[Int]] = Option(op5_p1 :: Nil)

//   }

//   implicit object ServiceCInterpreter extends ServiceC.Interpreter[Option] {

//     def op7Impl(op7_p1: Int): Option[List[Int]] = Option(op7_p1 :: Nil)

//     def op8Impl(op8_p1: Int): Option[List[Int]] = Option(op8_p1 :: Nil)

//   }

//   implicit object ServiceDInterpreter extends ServiceD.Interpreter[Option] {

//     def op10Impl(op7_p1: Int): Option[List[Int]] = Option(op7_p1 :: Nil)

//     def op11Impl(op8_p1: Int): Option[List[Int]] = Option(op8_p1 :: Nil)

//   }

// }

// object composition {

//   import definitions._

//   def program[F[_]](implicit P: Persistence[F]): Free[F, List[Int]] = {
//     import P.serviceA._, P.serviceB._, P.serviceC._
//     for {
//       a <- op1(1)
//       b <- op2(1)
//       c <- op3(1, 1)
//       d <- op4(1)
//       e <- op5(1)
//       f <- op6(1, 1)
//     } yield a ++ b ++ c ++ d ++ e ++ f
//   }
// }

// object Main {

//   import composition._
//   import definitions._

//   import cats.implicits._

//   def main(args: Array[String]): Unit = {

//     import io.freestyle.syntax._, runtimes._, App._
//     println(program[App.T].exec[Option])

//   }

// }

/*

object algebras {

  @free trait S1[F[_]] {
    def x(n: Int): Free[F, Int]
  }

  @free trait S2[F[_]] {
    def x(n: Int): Free[F, Int]
  }

  @free trait S3[F[_]] {
    def x(n: Int): Free[F, Int]
  }

  @free trait S4[F[_]] {
    def x(n: Int): Free[F, Int]
  }

}

object modules {

  import algebras._

  @module trait M1[F[_]] {
    val s1: S1[F]
    val s2: S2[F]
  }

  @module trait M2[F[_]] {
    val s3: S3[F]
    val s4: S4[F]
  }

  @module trait App[F[_]] {
    val m1: M1[F]
    val m2: M2[F]
  }

}

object interpreters {

  import algebras._

  implicit object S1Interpreter extends S1.Interpreter[Option] {
    def xImpl(n: Int) = Some(n)
  }

  implicit object S2Interpreter extends S2.Interpreter[Option] {
    def xImpl(n: Int) = Some(n)
  }
}

object main {

  import modules._
  import interpreters._
  import M1.implicits._
  import cats.implicits._
  import io.freestyle.syntax._

  def program[F[_]](implicit M: M1[F]): Free[F, Int] = {
    for {
      a <- M.s1.x(1)
      b <- M.s2.x(1)
    } yield a + b
  }

  program[M1.T].exec[Option]

}

 */
