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

import cats.{~>}

package cache {

  class KeyValueProvider[Key, Val] {

    /* CacheM trait is a type-class of functors for which key-value store operations
     * can be provided.
     *
     * We assume that the actual store is too big or remote to allow for general
     * operations over all values, or to search or to iterate over all keys
     */
    @free sealed trait CacheM {

      // Gets the value associated to a key, if there is one */
      def get(key: Key): OpPar[Option[Val]]

      // Sets the value of a key to a newValue.
      def put(key: Key, newVal: Val): OpPar[Unit]

      // Copy all of the mappings from the specified map to this cache
      def putAll(keyValues: Map[Key, Val]): OpPar[Unit]

      //If the specified key is not already associated with a value, associate it with the given value.
      def putIfAbsent(key: Key, newVal: Val): OpPar[Unit]

      // Removes the entry for the key if one exists
      def del(key: Key): OpPar[Unit]

      // Returns whether there is an entry for key or not.
      def has(key: Key): OpPar[Boolean]

      // Returns the set of keys in the store
      def keys: OpPar[List[Key]]

      // Removes all entries
      def clear: OpPar[Unit]

      //Replaces the entry for a key only if currently mapped to some value
      def replace(key: Key, newVal: Val): OpPar[Unit]

      //Returns true if this cache contains no key-value mappings.
      def isEmpty: OpPar[Boolean]

    }

    /*
     *  Ideal Equations for a CacheM. We use m,n for keys, v,w for values.
     *  Using different variables in an equation means that their values are different.
     *  We use `>>=` for `flatMap`, and `>>` for the sequence operator.
     *
     * For `put`:
     * - On a same key, only the right-most (latest) `put` counts:
     *      put(m,v) >> put(m,w) === put(m,w)
     * - `put` operations on different keys commute:
     *      put(m,v) >> put(n,w) === put(n,w) >> put(m,v)
     *
     * For `del`:
     * - Deletes on a same key are idempotent:
     *      del(m) >> del(m) === del(m)
     * - Deletes on different keys commute:
     *      del(m) >> del(n) === del(n) >> del(m)
     * - A put followed by a delete on a same key is the same as doing nothing
     *      put(m,v) >> del(m)   === return Unit
     * - A del followed by a put on a same key is the same as the put
     *      del(m) >> put(m,v)   === put(m,v)
     * - Del and put on different keys commute
     *      put(m,v) >> del(n) === del(n) >> put(m, v)
     *
     * For `get`:
     * - The result of a `get` should be that of the latest `put`
     *      put(m,v) >> get(m) === put(m,v) >> return( Some(v) )
     * - The result of a `get` after a `del` is Nothing
     *      del(m)  >> get(m)  === del(m) >> return(None)
     * - `get` commutes with `del` and `put` on different keys:
     *      put(m,v) >> get(n) === get(n) >>= (w => (put(m,v) >>= return(w) ))
     *      del(m)   >> get(n) === get(n) >>= (w => (del(m)   >>= return(w) )
     *
     */

    object implicits {
      implicit def cacheHandler[F[_], G[_]](
          implicit rawMap: KeyValueMap[F, Key, Val],
          interpret: F ~> G
      ): CacheM.Handler[G] = new CacheHandler[F, G]

      private[this] class CacheHandler[F[_], G[_]](
          implicit rawMap: KeyValueMap[F, Key, Val],
          interpret: F ~> G
      ) extends CacheM.Handler[G] {

        override def get(key: Key): G[Option[Val]] =
          interpret(rawMap.get(key))
        override def put(key: Key, newVal: Val): G[Unit] =
          interpret(rawMap.put(key, newVal))
        override def putAll(keyValues: Map[Key, Val]):G[Unit] =
          interpret(rawMap.putAll(keyValues))
        override def putIfAbsent(key: Key, newVal: Val) : G[Unit]=
          interpret(rawMap.putIfAbsent(key,newVal))
        override def del(key: Key): G[Unit] =
          interpret(rawMap.delete(key))
        override def has(key: Key): G[Boolean] =
          interpret(rawMap.hasKey(key))
        override def keys: G[List[Key]] =
          interpret(rawMap.keys)
        override def clear: G[Unit] =
          interpret(rawMap.clear)
        override def replace(key: Key,newVal: Val): G[Unit] =
          interpret(rawMap.replace(key,newVal))
        override def isEmpty: G[Boolean] =
          interpret(rawMap.isEmpty)
      }

    }
  }

  trait KeyValueMap[F[_], Key, Val] {

    def get(key: Key): F[Option[Val]]

    def put(key: Key, newVal: Val): F[Unit]

    def putAll(keyValues: Map[Key, Val]) : F[Unit]

    def putIfAbsent(key: Key, newVal: Val) : F[Unit]

    def delete(key: Key): F[Unit]

    def hasKey(key: Key): F[Boolean]

    def keys: F[List[Key]]

    def clear: F[Unit]

    def replace(key: Key, newVal: Val) : F[Unit]

    def isEmpty: F[Boolean]
  }

}

package object cache {
  def apply[Key, Val] = new KeyValueProvider[Key, Val]
}
