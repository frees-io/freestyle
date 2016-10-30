package io.freestyle

import cats.free.{Free, Inject}

object definitions {

  @free sealed abstract class ServiceA[F[_]] {

    def a(op1_p1: Int): Free[F, List[Int]]

  }

  @free sealed trait ServiceB[F[_]] {

    def b(op4_p1: Int): Free[F, List[Int]]

  }

  @free sealed trait ServiceC[F[_]] {

    def c(op4_p1: Int): Free[F, List[Int]]

  }

  @free sealed trait ServiceD[F[_]] {

    def d(op4_p1: Int): Free[F, List[Int]]

  }

}

object layers {

  import definitions._

  @module trait Persistence[F[_]] {

    val serviceA: ServiceA[F]

    val serviceB : ServiceB[F]

  }

  @module trait BizLogic[F[_]] {

    val serviceC: ServiceC[F]

    val serviceD : ServiceD[F]

  }

}


object app {

  import layers._
  import definitions._

  @module trait App[F[_]] {

    val persistence: Persistence[F]

    val bizLogic: BizLogic[F]

  }

}

object runtimes {

  import definitions._

  implicit object ServiceAInterpreter extends ServiceA.Interpreter[Option] {

    def aImpl(op1_p1: Int): Option[List[Int]] = Option(op1_p1 :: Nil)

  }

  implicit object ServiceBInterpreter extends ServiceB.Interpreter[Option] {

    def bImpl(op4_p1: Int): Option[List[Int]] = Option(op4_p1 :: Nil)

  }

  implicit object ServiceCInterpreter extends ServiceC.Interpreter[Option] {

    def cImpl(op7_p1: Int): Option[List[Int]] = Option(op7_p1 :: Nil)

  }

  implicit object ServiceDInterpreter extends ServiceD.Interpreter[Option] {

    def dImpl(op7_p1: Int): Option[List[Int]] = Option(op7_p1 :: Nil)

  }

}

object composition {

  import definitions._
  import layers._
  import runtimes._
  import app._
  import App._
  import io.freestyle.syntax._
  import cats.implicits._

  /*
  def program[F[_]](implicit A: App[F]): Free[F, List[Int]] = {
    import A.persistence.serviceA._, A.persistence.serviceB._, A.persistence.serviceC._
    for {
      a <- op1(1)
      b <- op2(1)
      c <- op3(1, 1)
      d <- op4(1)
      e <- op5(1)
      f <- op6(1, 1)
    } yield a ++ b ++ c ++ d ++ e ++ f
  }*/


  def main(args: Array[String]): Unit = {
    //val x = Persistence[App.T]
    /*
    import cats.implicits._
    import App._
    implicit val serviceAInject = Inject[ServiceA.T, App.T]
    implicit val serviceA = ServiceA.defaultInstance[App.T]
    implicit val persistence = Persistence.defaultInstance[App.T]
     */
    val program = App[App.T.T].persistence.serviceA.a(1)
    println(program[App.T.T].exec[Option])
  }

}


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
