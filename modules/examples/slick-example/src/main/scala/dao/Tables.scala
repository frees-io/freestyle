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

package dao
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.PostgresProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = userAddress.schema ++ userData.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table UserAddress
   *  @param userId Database column user_id SqlType(int4), PrimaryKey
   *  @param street Database column street SqlType(varchar), Length(32,true)
   *  @param city Database column city SqlType(varchar), Length(32,true)
   *  @param country Database column country SqlType(varchar), Length(32,true) */
  final case class UserAddressRow(userId: Int, street: String, city: String, country: String)

  /** GetResult implicit for fetching UserAddressRow objects using plain SQL queries */
  implicit def GetResultUserAddressRow(implicit e0: GR[Int], e1: GR[String]): GR[UserAddressRow] =
    GR { prs =>
      import prs._
      UserAddressRow.tupled((<<[Int], <<[String], <<[String], <<[String]))
    }

  /** Table description of table useraddress. Objects of this class serve as prototypes for rows in queries. */
  class UserAddress(_tableTag: Tag)
      extends profile.api.Table[UserAddressRow](_tableTag, "useraddress") {
    def * = (userId, street, city, country) <> (UserAddressRow.tupled, UserAddressRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      (Rep.Some(userId), Rep.Some(street), Rep.Some(city), Rep.Some(country)).shaped.<>(
        { r =>
          import r._; _1.map(_ => UserAddressRow.tupled((_1.get, _2.get, _3.get, _4.get)))
        },
        (_: Any) => throw new Exception("Inserting into ? projection not supported.")
      )

    /** Database column user_id SqlType(int4), PrimaryKey */
    val userId: Rep[Int] = column[Int]("user_id", O.PrimaryKey)

    /** Database column street SqlType(varchar), Length(32,true) */
    val street: Rep[String] = column[String]("street", O.Length(32, varying = true))

    /** Database column city SqlType(varchar), Length(32,true) */
    val city: Rep[String] = column[String]("city", O.Length(32, varying = true))

    /** Database column country SqlType(varchar), Length(32,true) */
    val country: Rep[String] = column[String]("country", O.Length(32, varying = true))

    /** Foreign key referencing UserData (database name id_fk) */
    lazy val userdataFK = foreignKey("id_fk", userId, userData)(
      r => r.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade)
  }

  /** Collection-like TableQuery object for table UserAddress */
  lazy val userAddress = new TableQuery(tag => new UserAddress(tag))

  /** Entity class storing rows of table UserData
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param email Database column email SqlType(varchar), Length(64,true)
   *  @param username Database column username SqlType(varchar), Length(32,true)
   *  @param age Database column age SqlType(int4), Default(None) */
  final case class UserDataRow(id: Int, email: String, username: String, age: Option[Int] = None)

  /** GetResult implicit for fetching UserDataRow objects using plain SQL queries */
  implicit def GetResultUserDataRow(
      implicit e0: GR[Int],
      e1: GR[String],
      e2: GR[Option[Int]]): GR[UserDataRow] = GR { prs =>
    import prs._
    UserDataRow.tupled((<<[Int], <<[String], <<[String], <<?[Int]))
  }

  /** Table description of table userdata. Objects of this class serve as prototypes for rows in queries. */
  class UserData(_tableTag: Tag) extends profile.api.Table[UserDataRow](_tableTag, "userdata") {
    def * = (id, email, username, age) <> (UserDataRow.tupled, UserDataRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      (Rep.Some(id), Rep.Some(email), Rep.Some(username), age).shaped.<>({ r =>
        import r._; _1.map(_ => UserDataRow.tupled((_1.get, _2.get, _3.get, _4)))
      }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

    /** Database column email SqlType(varchar), Length(64,true) */
    val email: Rep[String] = column[String]("email", O.Length(64, varying = true))

    /** Database column username SqlType(varchar), Length(32,true) */
    val username: Rep[String] = column[String]("username", O.Length(32, varying = true))

    /** Database column age SqlType(int4), Default(None) */
    val age: Rep[Option[Int]] = column[Option[Int]]("age", O.Default(None))
  }

  /** Collection-like TableQuery object for table UserData */
  lazy val userData = new TableQuery(tag => new UserData(tag))
}
