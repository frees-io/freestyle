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

  implicit def mapperFromFreeSParOutputHFunction[Op[_], A, B, F, FPOB](f: F)(implicit
    ftp: FnToProduct.Aux[F, (A) => FPOB],
    ev: FPOB <:< FreeS.Par[Op, Output[B]],
    I: Op ~> Future
  ): Mapper.Aux[A, B] = Mapper.mapperFromFutureOutputFunction(a => ev(ftp(f)(a)).freeS.exec[Future])

  implicit def mapperFromFreeSParOutputHFunctionId[Op[_], A, B, F, FPOB](f: F)(implicit
    ftp: FnToProduct.Aux[F, (A) => FPOB],
    ev: FPOB <:< FreeS.Par[Op, Output[B]],
    I: Op ~> Id
  ): Mapper.Aux[A, B] = Mapper.mapperFromOutputFunction(a => ev(ftp(f)(a)).freeS.exec[Id])

  implicit def mapperFromFreeSOutputValue[Op[_], A](fo: FreeS[Op, Output[A]])(implicit I: Op ~> Future): Mapper.Aux[HNil, A] =
    Mapper.mapperFromFutureOutputValue(fo.exec[Future])

  implicit def mapperFromFreeSOutputValueId[Op[_], A](fo: FreeS[Op, Output[A]])(implicit I: Op ~> Id): Mapper.Aux[HNil, A] =
    Mapper.mapperFromOutputValue(fo.exec[Id])

  implicit def mapperFromFreeSParOutputValue[Op[_], A](fpo: FreeS.Par[Op, Output[A]])(implicit I: Op ~> Future): Mapper.Aux[HNil, A] =
    mapperFromFreeSOutputValue(fpo.freeS)

  implicit def mapperFromFreeSParOutputValueId[Op[_], A](fpo: FreeS.Par[Op, Output[A]])(implicit I: Op ~> Id): Mapper.Aux[HNil, A] =
    mapperFromFreeSOutputValueId(fpo.freeS)

}

trait FinchMapperFreeS1 {

  implicit def mapperFromFreeSOutputFunction[Op[_], A, B](f: A => FreeS[Op, Output[B]])(implicit I: Op ~> Future): Mapper.Aux[A, B] =
    Mapper.mapperFromFutureOutputFunction(a => f(a).exec[Future])

  implicit def mapperFromFreeSOutputFunctionId[Op[_], A, B](f: A => FreeS[Op, Output[B]])(implicit I: Op ~> Id): Mapper.Aux[A, B] =
    Mapper.mapperFromOutputFunction(a => f(a).exec[Id])

  implicit def mapperFromFreeSParOutputFunction[Op[_], A, B](f: A => FreeS.Par[Op, Output[B]])(implicit I: Op ~> Future): Mapper.Aux[A, B] =
    mapperFromFreeSOutputFunction(f.andThen(_.freeS))

  implicit def mapperFromFreeSParOutputFunctionId[Op[_], A, B](f: A => FreeS.Par[Op, Output[B]])(implicit I: Op ~> Id): Mapper.Aux[A, B] =
    mapperFromFreeSOutputFunctionId(f.andThen(_.freeS))

}
