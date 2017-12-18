---
layout: docs
title: Cassandra
permalink: /docs/cassandra
---


# Freestyle-Cassandra

[Cassandra] atop **Freestyle** is **`frees-cassandra`**.
Freestyle Cassandra is Scala Purely Functional driver for Cassandra based on the datastax Java Driver.


<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

  - [What’s frees-cassandra](#whats-frees-cassandra)
  - [Installation](#installation)
  - [About Freestyle Cassandra](#about-freestyle-cassandra)
  - [Public APIs](#public-apis)
    - [ClusterAPI](#clusterapi)
    - [SessionAPI](#sessionapi)
    - [StatementAPI](#statementapi)
    - [ResultSetAPI](#resultsetapi)
  - [Features](#features)
    - [Low level Queries](#low-level-queries)
    - [String Interpolator](#string-interpolator)
- [References](#references)
- [freestyle-cassandra-examples](#freestyle-cassandra-examples)
  - [LowLevelApi](#lowlevelapi)
  - [StringQueryInterpolator](#stringqueryinterpolator)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## What’s frees-cassandra

[frees-cassandra] is a library to interact with cassandra built atop Free and using the Datastax 
Cassandra Driver for connecting to a Cassandra instance. It follows the [Freestyle] philosophy, 
being macro-powered. 

## Installation

Add the following resolver and library dependency to your project's build file. 

For Scala `2.11.x` and `2.12.x`:

```scala
Resolver.bintrayRepo("tabdulradi", "maven")
libraryDependencies += "io.frees" %% "frees-cassandra" % "0.0.5" 
```

## About Freestyle Cassandra

Freestyle-Cassandra provides 2 different ways of use, a low level one, letting you define a query 
and bind values to it, and a String Interpolator one, which allows us to write raw queries and 
validate them at compile time against a previously defined schema.

In the upcoming sections, we'll take a look at both features and how we can take advantage of them.

## Public APIs
*Frees-Cassandra* provides a set of [algebras](http://frees.io/docs/core/algebras/) to interact with 
the different pieces of a Cassandra Service. Each of those algebras represent a public API.   

### ClusterAPI
It provides methods to open/close connections to a Cassandra instance, load specific keyspaces, or 
Cassandra configuration.

```tut:silent

import com.datastax.driver.core.{Configuration, Metadata, Metrics, Session}
import freestyle.free._

@free
trait ClusterAPI {

  def connect: FS[Session]

  def connectKeyspace(keyspace: String): FS[Session]

  def close: FS[Unit]

  def configuration: FS[Configuration]

  def metadata: FS[Metadata]

  def metrics: FS[Metrics]

}

``` 

### SessionAPI
Provides a way to interact with a proper query. We can both define a query as a template - containing 
placeholders for real values - or a raw query string. Bear in mind that running a raw query using the 
SessionAPI directly is unsafe and does not check query correctness at compile time. 

```scala

import freestyle.free._
import com.datastax.driver.core._
import freestyle.cassandra.query.model.SerializableValueBy

@free
trait SessionAPI {

  def init: FS[Session]

  def close: FS[Unit]

  def prepare(query: String): FS[PreparedStatement]

  def prepareStatement(statement: RegularStatement): FS[PreparedStatement]

  def execute(query: String): FS[ResultSet]

  def executeWithValues(query: String, values: Any*): FS[ResultSet]

  def executeWithMap(query: String, values: Map[String, AnyRef]): FS[ResultSet]

  def executeStatement(statement: Statement): FS[ResultSet]

  def executeWithByteBuffer(
      query: String,
      values: List[SerializableValueBy[Int]],
      consistencyLevel: Option[ConsistencyLevel] = None): FS[ResultSet]

}

```

### StatementAPI
Provides methods to bind real query values to an already existing PreparedStatement, returning a 
BoundStatement, which can now be ran in a safe way.

```scala

import com.datastax.driver.core._
import freestyle.free._
import freestyle.cassandra.codecs.ByteBufferCodec
import freestyle.cassandra.query.model.SerializableValueBy

import java.nio.ByteBuffer

@free
trait StatementAPI {

  def bind(preparedStatement: PreparedStatement): FS[BoundStatement]

  def setByteBufferByIndex(
      boundStatement: BoundStatement,
      index: Int,
      bytes: ByteBuffer): FS[BoundStatement]

  def setByteBufferByName(
      boundStatement: BoundStatement,
      name: String,
      bytes: ByteBuffer): FS[BoundStatement]

  def setValueByIndex[T](
      boundStatement: BoundStatement,
      index: Int,
      value: T,
      codec: ByteBufferCodec[T]): FS[BoundStatement]

  def setValueByName[T](
      boundStatement: BoundStatement,
      name: String,
      value: T,
      codec: ByteBufferCodec[T]): FS[BoundStatement]

  def setByteBufferListByIndex(
      preparedStatement: PreparedStatement,
      values: List[SerializableValueBy[Int]]): FS[BoundStatement]

  def setByteBufferListByName(
      preparedStatement: PreparedStatement,
      values: List[SerializableValueBy[String]]): FS[BoundStatement]
}
```

### ResultSetAPI
Provides methods to interact directly with a Cassandra ResultSet, so we can get the final Object 
representation, a list of them, or describe that that ResultSet could not have been found in the 
current Cassandra instance.

```scala

import com.datastax.driver.core.ResultSet
import freestyle.free._
import freestyle.cassandra.query.mapper.FromReader

@free
trait ResultSetAPI {

  def read[A](resultSet: ResultSet)(implicit FR: FromReader[A]): FS[A]

  def readOption[A](resultSet: ResultSet)(implicit FR: FromReader[A]): FS[Option[A]]

  def readList[A](resultSet: ResultSet)(implicit FR: FromReader[A]): FS[List[A]]

}
```

## Features

### Low level Queries
The Low-Level API allows us to define queries with placeholders for query constraints, which can be 
later bound to the final values, running the query in a safe mode, preventing CQL injection. 
Frees-Cassandra checks the generated query is a valid one at compile time, throwing errors in case 
any requested value, table or keyspace names does not exist at the schema definition. 

Let's see how we can use it:

```scala
import java.util.UUID

import com.datastax.driver.core._
import freestyle.free._
import freestyle.free.implicits._
import freestyle.cassandra.implicits._
import freestyle.cassandra.api.QueryModule
import freestyle.cassandra.codecs._

final case class User(id: java.util.UUID, name: String)

def program[F[_]](implicit app: QueryModule[F]): FreeS[F, User] = {

import app._

implicit val stringTypeCodec: TypeCodec[String] = TypeCodec.varchar()
implicit val uuidTypeCodec: TypeCodec[UUID] = TypeCodec.uuid()
implicit val protocolVersion: ProtocolVersion = ProtocolVersion.V4

val newUser = User(UUID.randomUUID(), "Username")

def bindValues(st: PreparedStatement)(
  implicit c1: ByteBufferCodec[UUID],
  c2: ByteBufferCodec[String]): FreeS[F, BoundStatement] =
  List(
    statementAPI.setValueByName(_: BoundStatement, "id", newUser.id, c1),
    statementAPI.setValueByName(_: BoundStatement, "name", newUser.name, c2))
    .foldLeft[FreeS[F, BoundStatement]](statementAPI.bind(st)) { (freeS, func) =>
    freeS.flatMap(boundSt => func(boundSt))
  }

for {
  preparedStatement <- sessionAPI.prepare("INSERT INTO users (id, name) VALUES (?, ?)")
  boundStatement    <- bindValues(preparedStatement)
  _                 <- sessionAPI.executeStatement(boundStatement)
  user <- sessionAPI.executeWithMap(
    s"SELECT id, name FROM users WHERE id = ?",
    Map("id" -> newUser.id)) map { rs =>
    val row = rs.one()
    User(row.getUUID(0), row.getString(1))
  }
  _ <- sessionAPI.close
} yield user

}

```

### String Interpolator
Frees-Cassandra Query Interpolator allows us to write a raw query and validate it against a 
previously defined schema. 

To do so, we need to first define an annotated trait specifying a path to the file containing the
cassandra keyspaces & tables schemas. 

Example

```scala

import freestyle.cassandra.query.interpolator.MacroInterpolator.SchemaFileInterpolator

object Model {
    @SchemaFileInterpolator("/schema.cql")
    trait SchemaInterpolator
}
```

This annotation will be expanded via macros defining a cql string interpolator method, which
will allow us validate our queries against the real schema defined in a CQL script. 
Now we can import this SchemaInterpolator and take advantage of this macro generated utilities[1].  

Let's see an example. We can define an algebra to represent operations over an user, and a 
module to interact with other algebras:

```scala
import java.util.UUID

import com.datastax.driver.core.ResultSet
import freestyle.free._
import freestyle.cassandra.api._
import freestyle.free.logging._

@free
trait UserAPI {

  def insert(userId: UUID): FS[ResultSet]

  def get(userId: UUID): FS[ResultSet]
}

@module trait StringInterpolatorApp {
  val queryModule: QueryModule
  val log: LoggingM
  val userApi: UserAPI
}

```

Then, we need to define a handler for that algebra:

```
import java.util.UUID

import cats.MonadError
import com.datastax.driver.core.{ResultSet, Session}
import freestyle.async.AsyncContext
import freestyle.cassandra.query.interpolator._
import freestyle.cassandra.api._
import Model.SchemaInterpolator

import scala.concurrent.ExecutionContext

object implicits {

  implicit val stringTypeCodec: TypeCodec[String] = TypeCodec.varchar()
  implicit val uuidTypeCodec: TypeCodec[UUID] = TypeCodec.uuid()
  implicit val protocolVersion: ProtocolVersion = ProtocolVersion.V4

  implicit def userApiHandler[F[_]](implicit API: SessionAPI[SessionAPI.Op],
    S: Session,
    AC: AsyncContext[F],
    E: ExecutionContext,
    ME: MonadError[F, Throwable]): UserAPI.Handler[F] = new UserAPI.Handler[F] {

    import SchemaInterpolator._

    override protected[this] def insert(userId: UUID): F[ResultSet] =
      cql"INSERT INTO demodb.users (id, name) VALUES ($userId, 'Username');".attemptResultSet[F]()

    override protected[this] def get(userId: UUID): F[ResultSet] =
      cql"SELECT id, name FROM demodb.users WHERE id = $userId".attemptResultSet[F]()
  }

}
```

So, now we can compose those algebras ops easily:

```scala

import java.util.UUID

import com.datastax.driver.core._
import freestyle.free._
import freestyle.free.implicits._
import freestyle.cassandra.api._
import freestyle.cassandra.implicits._
import freestyle.cassandra.query.interpolator._

object Example extends App {

  final case class User(id: java.util.UUID, name: String)

  val uuid = UUID.randomUUID()

  def program[F[_]](implicit app: StringInterpolatorApp[F]): FreeS[F, User] = {
  
      implicit val s = app.queryModule.sessionAPI
  
      for {
        _             <- app.log.debug(s"# Executing insert query with id $uuid")
        _             <- app.userApi.insert(uuid)
        _             <- app.log.debug("# Selecting previous inserted item")
        userResultSet <- app.userApi.get(uuid)
        user = {
          val userRow = userResultSet.one()
          User(userRow.getUUID(0), userRow.getString(1))
        }
        _             <- app.log.debug(s"# Fetched item: $user")
        _             <- app.log.debug(s"# Closing connection")
        _             <- app.queryModule.sessionAPI.close
      } yield user
    }

}
```

[1] Note that we need to define this trait in a different compilation unit (e.g.: a different SBT 
module) since we are using Contextual Macro Interpolator, so a new macro is defined by our macro.

# References

* [Freestyle](http://frees.io/)
  Frestyle Cassandra is part of the ecosystem built around Freestyle, and makes an intensive use of it.
  
* [Troy](https://github.com/schemasafe/troy)
  We are currently using Troy for inferring the schema from a CQL script file.

* [Contextual](https://github.com/propensive/contextual)
  It helps us to create string interpolators.
  
* [Datastax Driver](http://docs.datastax.com/en/developer/driver-matrix/doc/common/driverMatrix.html)
  Interactions with Cassandra clusters are done through the Java Datastax Cassandra Driver.

# freestyle-cassandra-examples

All code examples are available in [Github](https://github.com/frees-io/freestyle-cassandra-example).

## [LowLevelApi](https://github.com/frees-io/freestyle-cassandra-example/blob/master/basic/examples/src/main/scala/freestyle/cassandra/sample/LowLevelApi.scala)
## [StringQueryInterpolator](https://github.com/frees-io/freestyle-cassandra-example/blob/master/basic/examples/src/main/scala/freestyle/cassandra/sample/StringInterpolator.scala)