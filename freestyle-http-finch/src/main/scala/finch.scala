package freestyle
package http

import cats.{~>, Id}
import com.twitter.util.Future
import io.catbird.util._
import io.finch.Output
import io.finch.internal.Mapper
import shapeless.HNil
import shapeless.ops.function.FnToProduct

import freestyle.implicits._

object finch extends FinchMapperFrees


trait FinchMapperFrees extends FinchMapperFreeS1 {

  implicit def mapperFromFreeSOutputHFunction[Op[_], A, B, F, FOB](f: F)(implicit 
    ftp: FnToProduct.Aux[F, (A) => FOB], 
    ev: FOB <:< FreeS[Op, Output[B]],
    I: Op ~> Future
  ): Mapper.Aux[A, B] = Mapper.mapperFromFutureOutputFunction(a => ev(ftp(f)(a)).exec[Future]) 

  implicit def mapperFromFreeSOutputHFunctionId[Op[_], A, B, F, FOB](f: F)(implicit 
    ftp: FnToProduct.Aux[F, (A) => FOB], 
    ev: FOB <:< FreeS[Op, Output[B]],
    I: Op ~> Id
  ): Mapper.Aux[A, B] = Mapper.mapperFromOutputFunction(a => ev(ftp(f)(a)).exec[Id]) 

}

trait FinchMapperFreeS1 {

  implicit def mapperFromFreeSOutputValue[Op[_], A](fo: => FreeS[Op, Output[A]])(implicit I: Op ~> Future): Mapper.Aux[HNil, A] =
    Mapper.mapperFromFutureOutputValue(fo.exec[Future])

  implicit def mapperFromFreeSOutputValueId[Op[_], A](fo: => FreeS[Op, Output[A]])(implicit I: Op ~> Id): Mapper.Aux[HNil, A] =
    Mapper.mapperFromOutputValue(fo.exec[Id])

  implicit def mapperFromFreeSOutputFunction[Op[_], A, B](f: A => FreeS[Op, Output[B]])(implicit I: Op ~> Future): Mapper.Aux[A, B] =
    Mapper.mapperFromFutureOutputFunction(a => f(a).exec[Future])

  implicit def mapperFromFreeSOutputFunctionId[Op[_], A, B](f: A => FreeS[Op, Output[B]])(implicit I: Op ~> Id): Mapper.Aux[A, B] =
    Mapper.mapperFromOutputFunction(a => f(a).exec[Id])

}


object Test {
  import io.finch._
  import freestyle.http.finch._

  import shapeless.{::, HNil}

  import algebra._
  import handler._

  val threeId:    Endpoint[Int] = / { Ok(1 + 2) }
  val threeFut:   Endpoint[Int] = / { Future.value(Ok(1 + 2)) }
  val threeFreeS: Endpoint[Int] = / { Calc[Calc.Op].sum(1, 2).map(Ok(_)) }

  val incrId:    Endpoint[Int] = int { i: Int => Ok(i + 1) }
  val incrFut:   Endpoint[Int] = int { i: Int => Future.value(Ok(i + 1)) }
  val incrFreeS: Endpoint[Int] = int { i: Int => Calc[Calc.Op].sum(i, 1).map(Ok(_)) }

  val both: Endpoint[Int :: Int :: HNil] = int :: int
  val sumId:    Endpoint[Int] = both { (a: Int, b: Int) => Ok(a + b) }
  val sumFut:   Endpoint[Int] = both { (a: Int, b: Int) => Future.value(Ok(a + b)) }
  val sumFreeS: Endpoint[Int] = both { (a: Int, b: Int) => Calc[Calc.Op].sum(a, b).map(Ok(_)) }
}

object algebra {
  @free trait Calc[F[_]] {
    def sum(a: Int, b: Int): FreeS[F, Int]
  }
}

object handler {
  import algebra._

  implicit val calcFutureHandler: Calc.Handler[Future] =
    new Calc.Handler[Future] {
      def sum(a: Int, b: Int): Future[Int] = Future.value(a + b)
    }
}



object TestId {
  import io.finch._
  import freestyle.http.finch._

  import shapeless.{::, HNil}

  import algebra._
  import handlerId._

  val threeId:    Endpoint[Int] = / { Ok(1 + 2) }
  val threeFut:   Endpoint[Int] = / { Future.value(Ok(1 + 2)) }
  val threeFreeS: Endpoint[Int] = / { Calc[Calc.Op].sum(1, 2).map(Ok(_)) }

  val incrId:    Endpoint[Int] = int { i: Int => Ok(i + 1) }
  val incrFut:   Endpoint[Int] = int { i: Int => Future.value(Ok(i + 1)) }
  val incrFreeS: Endpoint[Int] = int { i: Int => Calc[Calc.Op].sum(i, 1).map(Ok(_)) }

  val both: Endpoint[Int :: Int :: HNil] = int :: int
  val sumId:    Endpoint[Int] = both { (a: Int, b: Int) => Ok(a + b) }
  val sumFut:   Endpoint[Int] = both { (a: Int, b: Int) => Future.value(Ok(a + b)) }
  val sumFreeS: Endpoint[Int] = both { (a: Int, b: Int) => Calc[Calc.Op].sum(a, b).map(Ok(_)) }
}


object handlerId {
  import algebra._

  implicit val calcIdHandler: Calc.Handler[Id] =
    new Calc.Handler[Id] {
      def sum(a: Int, b: Int): Id[Int] = a + b
    }
}