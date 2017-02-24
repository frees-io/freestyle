---
layout: docs
title: Cache
permalink: /docs/effects/Cache/
---

## Cache

The `Cache` effect algebra allows to interact with a _global_ key-value data store.
The algebra declares several abstract operations that allow for reading and writing data into the store.
This algebra is parametrized on the types `K`, of keys used for the indexing, and on the type `V`, of values stored in the cache.

The algebra does not assume any specific implementation or representation of the data store.
That representation is given and specified by each _interpreter or handler_ for the algebra.
Simmilarly, the algebra imposes on the type parameters `K` and `V` no type constraint concerning
ordering, hashing, or encoding. We assume, though, that an equality test between their values exists.


### Operations

The set of abstract operations of the `Cache` algebra are specified as follows.

```Scala
class KeyValueProvider[Key, Value] {
    @free sealed trait CacheM[F[_]] {
      def get(key: Key):              FreeS.Par[F, Option[Val]]
      def put(key: Key, newVal: Val): FreeS.Par[F, Unit]
      def del(key: Key):              FreeS.Par[F, Unit]
      def has(key: Key):              FreeS.Par[F, Boolean]
      def keys:                       FreeS.Par[F, List[Key]]
      def clear:                      FreeS.Par[F, Unit]
    }
}
```
To make the algebra parametric on the types of `Key` and `Value`, we wrap the declaration of the algebra inside a `KeyValueProvider`; but note that this class has no instance data.

* `get(key: Key): M[Option[Val]]` issues a query to the data store on a given key. The result can be `None`, if the store has no mapping for that key, or `Some(v)` if the key is mapped to the value `v`.
* `put(key: Key, v: Value)` issues a command to
* `del(key: Key)` issues a command to disassociate a given key from the store, if it was present.
* `clear` is an abstract operation to disassociate all keys and values in the store.
* `keys` is a  query to retrieve a list of keys for which an association is present in the store. No restriction is posed on the order in which the keys will appear on the list, in relation to any ordering for the `Key` type.


### Laws and Equations

In this section we give some of the laws that specify the intended semantics of the `CacheM` algebra. This laws should hold in any handler/interpreter for it.
All of these laws are applicable in the context of a single group of `FreeS` independent operations, which we assume are run as a single batch of operations.
The key intuition behind these equations is that, within a single group of operations, every command issued to modify the key-value associations in the store must have an effect visible on succeeding operations in the same group.

The laws are written as equalities between monadic programs. Intuitively, equality between potentially side-effectful monadic programs means that:

* The (monad-wrapped) results from one computation is equal to those of the other; and
* their side-effects are non-distinguishable: there is no sequence of cache operations that, if attached to either program, would yield a different result.

In these laws, we use the binary operators `<*`, `*>`, and `|@|`, from the `cats.syntax.CartesianOps` trait in the `cats` library.
Briefly, for any two values `a : F[A]` and `b: F[B]`, `a *> b : F[B]` projects to the right, `a <* b: F[A]` projects on the left, and `a |@| b: F[(A,B)]` keeps a tuple of the results.
We use the variables `x` and `y` to denote keys of type `Key`, with `x != y`, and we use `v`, or `w` to denote values of type `Value`.

#### Get

Intuitively, a `get` abstract operation is just a query and should not modify the associations in the store.

This can be expressed with two conditions:

1. Any `get` operation whose result is ignored can be discarded from the program.
2. Consecutive `get` operations on a same key `x` should give the same result.
3. Consecutive `get` operations on different keys should give the same result for each key.

```Scala
get(x) *> ff   === ff <* get(x) === ff   // 1, for any other operation ff
get(x) |@| get(x)  === get( x).map( dup )
get(x) |@| get(y)  === ( get(y) |@| get(x) ).map( swap )
```

In the previous conditions we use the functions `swap: (A,B) => (B,A)` and `dup: A => (A,A)`; and we

#### Put

The `put` abstract operation on the keys of a key-value store is like an assignment to a mutable variable.
The following laws restrict `put` operations and `get` operations:

1. Two `put` operations on the same key is equivalent to the rightmost of these.
2. Two `put` operations on different keys can be swapped.
3. On a same key `x`, a `put` followed by the `get` is the same as giving the `put` value, right after the `put` operation.
4. Operations `get` and `put` on different keys can be swapped.

```Scala
put( x, v) *> put( x, w) === put( x, w)
put( x, v) *> put( y, w) === put( y, w) *> put( x, v)
put( x, v) *> get( x)    === put( x, v) *> pure( Some(v) )
put( x, v) *> get( y)    === get( y) <* put( x, v)
```

#### Delete

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

#### Keys, Clear

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

1. The list should only contain keys for which `get` would not give `None`.
2. The `keys` should not modify the data-store, so any `get` following it should give the same result.

```Scala
get(x).map(_.isDefined) === keys.map(_.contains(x) ) //1
keys *> get(x) === get(x)
```
