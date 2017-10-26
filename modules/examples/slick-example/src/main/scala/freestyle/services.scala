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

import cats.implicits._

import freestyle.slick._
import freestyle.slick.implicits._

import scala.concurrent.ExecutionContext
import persistence._
import dao.Tables._
import modules._

object services {
  def print[F[_]: SlickM](
      implicit example: Example[F],
      executionContext: ExecutionContext): FreeS[F, Unit] = {
    for {
      _            ← createSchema.liftFS[F]
      _            ← example.log.info("Created schema")
      id           ← insertUser(UserDataRow(0, "a@g.com", "a", Some(12))).liftFS[F]
      user         ← getUser(id).liftFS[F]
      _            ← example.log.info(s"Added $user")
      numUpdates   ← updateUser(UserDataRow(user.id, "ad@g.com", "ar", Some(24))).liftFS[F]
      _            ← example.log.info(s"Updates: $numUpdates")
      userList     ← listUser.liftFS[F]
      _            ← example.log.info(s"Users $userList")
      addressEmail ← insertAddress(UserAddressRow(user.id, "baker", "London", "UK")).liftFS[F]
      address      ← getAddress(addressEmail).liftFS[F]
      _            ← example.log.info(s"Added $address")
      numDeletes   ← deleteUser(user.id).liftFS[F]
      _            ← example.log.info(s"Deletes: $numDeletes")
      _            ← dropSchema.liftFS[F]
      _            ← example.log.info("Deleted schema")
    } yield ()
  }
}
