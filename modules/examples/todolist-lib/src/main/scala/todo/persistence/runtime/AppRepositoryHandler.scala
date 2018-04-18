/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package examples.todolist.persistence.runtime

import cats.Monad
import doobie.implicits._
import doobie.util.transactor.Transactor
import examples.todolist.model.AppModel
import examples.todolist.persistence.AppRepository

class AppRepositoryHandler[F[_]: Monad](implicit T: Transactor[F])
    extends AppRepository.Handler[F] {

  import examples.todolist.persistence.runtime.queries.AppQueries._

  def list: F[List[AppModel]] =
    listQuery.to[List].transact(T)
}
