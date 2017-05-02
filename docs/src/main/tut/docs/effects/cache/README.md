---
layout: docs
title: Cache
permalink: /docs/effects/Cache/
---

## Cache

The `Cache` effect algebra allows interacting with a _global_ key-value data store.
It declares several abstract operations to read and write data into the store.

In order to enable this algebra, you can depend on _freestyle-cache_:

[comment]: # (Start Replace)

```scala
libraryDependencies += "com.47deg" %% "freestyle-cache" % "0.1.0"
```

[comment]: # (End Replace)

This algebra is parametrized on the types `Key`, and `Val`, for keys and values in the store, respectively.

```Scala
class KeyValueProvider[Key, Val] {
  @free sealed trait CacheM {
    def get(key: Key):              FS[Option[Val]]
    def put(key: Key, newVal: Val): FS[Unit]
    def del(key: Key):              FS[Unit]
    def has(key: Key):              FS[Boolean]
    def keys:                       FS[List[Key]]
    def clear:                      FS[Unit]
  }
}
```

To create the algebra parametric on the types of `Key` and `Value`, we wrap the declaration of the algebra inside a `KeyValueProvider`; but note that this class has no instance data.
The algebra assumes no specific implementation or representation of the data store, since that is a matter for each _handler_ of the algebra.
For the same reason, it poses no type constraint on `Key` or `Val` with regards to ordering, hashing, or encoding, but it is assumed that equality is defined for both types.

### Using the Cache Effect

The following code snippet shows how to import and use the operations from the Cache algebra inside a program:

```tut:book
import freestyle._
import freestyle.implicits._
import freestyle.cache._
import cats.implicits._

val prov = new KeyValueProvider[Char, Int]

import prov.CacheM
import prov.implicits._

def loadFrom[F[_]: prov.CacheM] = {
  for {
    a <- 1.pure[FreeS[F, ?]]
    b <- CacheM[F].get('a')
    c <- 1.pure[FreeS[F, ?]]
  } yield a + b.getOrElse(0) + c
}
```

### Operations

The set of abstract operations of the `Cache` algebra are specified as follows:

* `get(key: Key): M[Option[Val]]` issues a query to the data store on a given key. The result can be `None`, if the store has no mapping for that key, or `Some(v)` if the key is mapped to the value `v`.
* `put(key: Key, v: Value)` issues a command to associate a given key to a given value
* `del(key: Key)` issues a command to disassociate a given key from the store, if it was present.
* `clear` is an abstract operation to disassociate all keys and values in the store.
* `keys` is a  query to retrieve a list of keys for which an association is present in the store. No restriction is posed on the order in which the keys will appear on the list, in relation to any ordering for the `Key` type.


#### Laws

In this section we describe some laws that specify the intended semantics of the `CacheM` effect algebra, which any handler or interpreter for it should hold.
These laws are applicable to any group of independent operations within the `FreeS.Par[F, ?]` type, which is the `Applicable` fragment.
However, the laws may not hold in the general `Free[FreeS.Par[F, ?], ?]` case, of a _sequential_ group of independent operations.
Intuitively, a group `FreeS.Par` of operations is run atomically on the data store, without interleaving any other operations, and every command's effect should be immediately visible on succeeding operations.

Each law is an _equality_ between two programs of type `FreeS.Par[F, A]`, where equality means that:

* The results of type `A` from interpreting both programs should be equal; and
* their side-effects on the data store are not distinguishable: there is no sequence `FreeS.Par` of `CacheM` foperations that,
  if attached to both programs to the left or to the right, would yield a different result.

In these laws, we use the binary operators `<*`, `*>`, and `|@|`, to indicate the composition and
projection of operations on the left, on the right, or on a pair. These operators come from the `cats.syntax.CartesianOps` trait.
We use variables `x` and `y` to denote keys of type `Key`, with `x != y`,  and we use `v`, or `w` to denote values of type `Value`.

##### Get

A `get` abstract operation is a query that should not modify the store.
This can be expressed with these conditions:

1. Any `get` operation whose result is ignored can be discarded from the program.
2. Consecutive `get` operations on a same key `x` should give the same result.
3. Consecutive `get` operations on different keys should give the same result for each key.

```Scala
get(x) *> ff   === ff <* get(x) === ff   // for any other operation ff
get(x) |@| get(x)  === get(x).map( dup )
get(x) |@| get(y)  === ( get(y) |@| get(x) ).map( swap )
```
Where `swap` is the function of type `(A,B) => (B,A)`, and `dup` is the function of type `A => (A,A)`.

##### Put

A `put` operation on a key in the store is like an assignment to a mutable variable.
The following laws restrict `put` operations and `get` operations:

1. Two `put` operations on the same key are equivalent to the rightmost (last) one.
2. Two `put` operations on different keys can be swapped.
3. On a same key `x`, a `put` followed by the `get` is the same as giving the `put` value, right after the `put` operation.
4. Operations `get` and `put` on different keys can be swapped.

```Scala
put( x, v) *> put( x, w) === put( x, w)
put( x, v) *> put( y, w) === put( y, w) *> put( x, v)
put( x, v) *> get( x)    === put( x, v) *> pure( Some(v) )
put( x, v) *> get( y)    === get( y) <* put( x, v)
```

##### Delete

The laws concerning `del` with `get` are the following ones:

* On a same key `x`, the result of a `del` right after a `get` should be `None`:
* On different keys `x` and `n`, a `del` and a `get` can be swapped:

```Scala
del( x) *> get( x) === del( x) *> pure( None)
del( x) *> get( y) === get( y) <* del( x)
```

The following laws concern several `delete` interactions, as well as `put` and `delete` operations.

1. Two `del` operations on a same key `x` are equivalent to just one of them.
2. Two `del` operations on different keys `x`, `y`, can be swapped.
3. On a same key `x`, a `put` followed by a delete should be equivalent to just the delete.
4. On a same key `x`, a `del` followed by a `put` should be equivalent to just the `put` operation.
5. Operations `put` and `del` on different keys can be reordered swapped.

```Scala
del( x) *> del( x) === del( x)                     // 1
del( x) *> del( y) === del( y) *> del( x)          // 2
put( x, v) *> del( x) === del( x)                  // 3
del( x, v) *> put( x, v) === put( x, v)            // 4
del( x, v) *> put( y, v) === put( y, v) *> del( x) // 5
```

##### Keys, Clear

The `clear` abstract operation is equivalent to issuing a `del` on all existing keys.

1. After a `clear`, a `get` should always return `None`:
2. After a `clear`, any `del` is redundant:
3. Any `put` operation right before a `clear` can be ignored:

```Scala
clear *> get( x) === clear *> pure( None)
clear *> del( x) === clear
put( x, v) *> clear === clear
```

The `keys` abstract operation, that returns a list of keys of type `Key`, must hold the following conditions:

1. The list should contain a key if and only if the `get` on that key would be `Some`.
2. The `keys` should not modify the data-store, so any `get` following it should give the same result.

```Scala
get(x).map(_.isDefined) === keys.map(_.contains(x) ) //1
keys *> get(x) === get(x)
```

### Handlers

#### Concurrent Hash Map

The package `freestyle.cache.hashmap` in the project `freestyle-cache`, contains an in-program (no I/O needed)
effect-handler / interpreter for the `CacheM` effect algebra.
This interpreter implements the data store with a [Concurrent Hash Map](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html).
Note that the `ConcurrentHashMap` provides no global locking, nor any facilities for issuing and executing transactionally a set of operations.
Thus, the laws described above may not hold for this interpreter. 

##### Hashable Type Class

A hash table divides keys according to each key's hash code. In particular, a `ConcurrentHashMap` uses the `hashCode()` method
from the [`Object`](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html) class ([`Any`](http://www.scala-lang.org/api/current/scala/Any.html) in Scala).
Most classes from the Java or Scala libraries define this method, and in Scala each `case class` has a definition based on its fields.
However, using this methods directly has two problems:

* Because `hashCode` is an object method, fixed by the class, we can not use different hash algorithms in different hash tables.
* Since `Object` provides a default implementation of `hashCode`, most programmer-defined classes may not override it.

To avoid these problems, we define a Scala typeclass `Hasher[A]`, i.e. a  trait with a single `hashCode` function, and for building the `CacheM` interpreter
we require an instance of `Hasher` for the `Key` type. We provide default implementations of this trait for several simple types.

#### Redis

The package `freestyle.cache.redis` in the project `freestyle-cache-redis` provides a handler-interpreter for the `CacheM` effect algebra.
This interpreter implements the data store using a [Redis](http://redis.io/) database.
Access to a Redis server is done through [`rediscala`](https://github.com/etaty/rediscala), an Akka-based client library for Scala.

##### Codification

In the `rediscala` client for Redis, the keys used for indexing values are always of type `String`; whereas
a special binary codification is needed for values (of type `Value`).
in the Redis integration we use a couple of typeclasses to transform either keys or values, to and from `String`:

* The `Format[A]` trait defines the function from the `Key` type (the parameter for the `CacheM` algebra) to `String`.
  The companion object provides a few helper methods and default instances.
* The `Parse[A]` trait is a function to try to _parse_ a `Key` from a `String`.
  The return type is an `Option[Key]`, to indicate the possibility of failure.

These typeclasses are used for transforming

The use of these typeclasses is also intended to avoid coupling the programs based on `freestyle` to a particular client library.

##### Batching and Transactions

As explained in the guide, any Freestyle program has two levels: the _applicative_ level (in type `FreeS.Par`)
contains sequences of locally independent operations, where the result of one can not be used as argument to another.
Such dependencies are only handled in the _monadic_ level, in which the results from a group of operations are retrieved
to be used in another group.

In  `Redis`, since each command is issued through the network, retrieving any value incurs in a round-trip latency.
This reduces performance, and exposes the results of fqueries to concurrent modifications of the data store.
To avoid these problems, we want the `Redis` handler to run any sequence of operations inside a `FreeS.Par` so that:

* Commands issued to the Redis server are _pipelined_, so as to reduce the total roundtrip to one; and
* the whole command sequence is run _atomically_, that is, without interleaving any other operation.

Regarding pipelining, the `rediscala` client already uses [Redis pipelining](https://redis.io/topics/pipelining) to
issue operations as soon as they are enqueued.
For atomicity, the implementation runs each `FreeS.Par` sequence of independent operations in a
Redis [_transaction_](https://redis.io/topics/transactions).
This allows the implementation to hold the laws for the `CacheM` effect algebra given above.

##### Kleisli

To combine several `CacheM` operations in a single `FreeS.Par`, we use the [`Kleisli`](https://github.com/typelevel/cats/blob/master/core/src/main/scala/cats/data/Kleisli.scala) class from the `cats` library.
Intuitively, for each single `CacheM` operation the interpreter does not _issue_ an operation to the client,
but instead creates a `Function` that, when applied to a client, will issue the command through it.

```Scala
def del[Key](keys: List[Key])(implicit format: Format[Key]): Ops[Future, Long] =
  Kleisli((client: KeyCommands) => client.del(keys.map(format): _*))
```

Given two such `Kleisli` objects that correspond to two independent operations, composing them can be done through the
default instance of `Applicative` for `Kleisli` in `cats`. The result would be a function that takes a  _client_ and issues
the commands for the operations in the order in which they appear.


