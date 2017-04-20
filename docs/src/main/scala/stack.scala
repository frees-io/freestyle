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

package freestyle.docs.stack

import freestyle._

object types {
  type CustomerId = java.util.UUID
}

import types._

case class Customer(id: CustomerId, name: String)
case class Order(crates: Int, variety: String, customerId: CustomerId)
case class Config(varieties: Set[String])

@free
trait CustomerPersistence {
  def getCustomer(id: CustomerId): FS[Option[Customer]]
}

@free
trait StockPersistence {
  def checkQuantityAvailable(variety: String): FS[Int]
  def registerOrder(order: Order): FS[Unit]
}

@module
trait Persistence[F[_]] {
  val customer: CustomerPersistence[F]
  val stock: StockPersistence[F]
}
