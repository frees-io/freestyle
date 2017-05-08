/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

private[http] trait FinchMapperFrees extends FinchMapperFreeS1 {

  implicit def mapperFromFreeSOutputHFunction[Op[_], A, B, F, FOB](f: F)(
      implicit ftp: FnToProduct.Aux[F, (A) => FOB],
      ev: FOB <:< FreeS[Op, Output[B]],
      I: Op ~> Future): Mapper.Aux[A, B] = mapperFromFreeSOutputFunction(a => ev(ftp(f)(a)))

  implicit def mapperFromFreeSOutputHFunctionId[Op[_], A, B, F, FOB](f: F)(
      implicit ftp: FnToProduct.Aux[F, (A) => FOB],
      ev: FOB <:< FreeS[Op, Output[B]],
      I: Op ~> Id): Mapper.Aux[A, B] = mapperFromFreeSOutputFunctionId(a => ev(ftp(f)(a)))

  implicit def mapperFromFreeSParOutputHFunction[Op[_], A, B, F, FPOB](f: F)(
      implicit ftp: FnToProduct.Aux[F, (A) => FPOB],
      ev: FPOB <:< FreeS.Par[Op, Output[B]],
      I: Op ~> Future): Mapper.Aux[A, B] = mapperFromFreeSOutputFunction(a => ev(ftp(f)(a)).freeS)

  implicit def mapperFromFreeSParOutputHFunctionId[Op[_], A, B, F, FPOB](f: F)(
      implicit ftp: FnToProduct.Aux[F, (A) => FPOB],
      ev: FPOB <:< FreeS.Par[Op, Output[B]],
      I: Op ~> Id): Mapper.Aux[A, B] = mapperFromFreeSOutputFunctionId(a => ev(ftp(f)(a)).freeS)

  implicit def mapperFromFreeSOutputValue[Op[_], A](fo: FreeS[Op, Output[A]])(
      implicit I: Op ~> Future): Mapper.Aux[HNil, A] =
    Mapper.mapperFromFutureOutputValue(fo.interpret[Future])

  implicit def mapperFromFreeSOutputValueId[Op[_], A](fo: FreeS[Op, Output[A]])(
      implicit I: Op ~> Id): Mapper.Aux[HNil, A] =
    Mapper.mapperFromOutputValue(fo.interpret[Id])

  implicit def mapperFromFreeSParOutputValue[Op[_], A](fpo: FreeS.Par[Op, Output[A]])(
      implicit I: Op ~> Future): Mapper.Aux[HNil, A] =
    mapperFromFreeSOutputValue(fpo.freeS)

  implicit def mapperFromFreeSParOutputValueId[Op[_], A](fpo: FreeS.Par[Op, Output[A]])(
      implicit I: Op ~> Id): Mapper.Aux[HNil, A] =
    mapperFromFreeSOutputValueId(fpo.freeS)

}

private[http] trait FinchMapperFreeS1 {

  implicit def mapperFromFreeSOutputFunction[Op[_], A, B](f: A => FreeS[Op, Output[B]])(
      implicit I: Op ~> Future): Mapper.Aux[A, B] =
    Mapper.mapperFromFutureOutputFunction(a => f(a).interpret[Future])

  implicit def mapperFromFreeSOutputFunctionId[Op[_], A, B](f: A => FreeS[Op, Output[B]])(
      implicit I: Op ~> Id): Mapper.Aux[A, B] =
    Mapper.mapperFromOutputFunction(a => f(a).interpret[Id])

  implicit def mapperFromFreeSParOutputFunction[Op[_], A, B](f: A => FreeS.Par[Op, Output[B]])(
      implicit I: Op ~> Future): Mapper.Aux[A, B] =
    mapperFromFreeSOutputFunction(f.andThen(_.freeS))

  implicit def mapperFromFreeSParOutputFunctionId[Op[_], A, B](f: A => FreeS.Par[Op, Output[B]])(
      implicit I: Op ~> Id): Mapper.Aux[A, B] =
    mapperFromFreeSOutputFunctionId(f.andThen(_.freeS))

}
