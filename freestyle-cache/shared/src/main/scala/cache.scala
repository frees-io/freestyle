package freestyle

package cache {

  class KeyValueProvider[Key, Val] {

    /* CacheM trait is a type-class of functors for which key-value store operations
     * can be provided.
     *
     * We assume that the actual store is too big or remote to allow for general
     * operations over all values, or to search or to iterate over all keys
     */
    @free sealed trait CacheM[F[_]] {

      // Gets the value associated to a key, if there is one */
      def get(key: Key): FreeS.Par[F, Option[Val]]

      // Sets the value of a key to a newValue.
      //  @returns: Was there a previous entry for `key`? */
      def put(key: Key, newVal: Val): FreeS.Par[F, Unit]

      // Removes the entry for the key if one exists
      // @returns: Was there a previous entry for `key`?
      def del(key: Key): FreeS.Par[F, Unit]

      // Returns whether there is an entry for key or not.
      def has(key: Key): FreeS.Par[F, Boolean]

      // Removes all entries
      def clear: FreeS.Par[F, Unit]
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

      import cats.Functor
      import hashmap._

      private[this] class HashMapCacheInterpreter[M[_]](
          implicit hasher: Hasher[Key],
          C: Capture[M],
          F: Functor[M]
      ) extends CacheM.Interpreter[M] {

        private[this] val wrapper: ConcurrentHashMapWrapper[Key, Val] =
          new ConcurrentHashMapWrapper[Key, Val]()

        override def getImpl(key: Key): M[Option[Val]] =
          C.capture(wrapper.get(key))
        override def putImpl(key: Key, newVal: Val): M[Unit] =
          F.void(C.capture(wrapper.put(key, newVal)))
        override def delImpl(key: Key): M[Unit] =
          F.void(C.capture(wrapper.delete(key)))
        override def hasImpl(key: Key): M[Boolean] =
          C.capture(wrapper.get(key).isDefined)
        override def clearImpl: M[Unit] =
          F.void(C.capture(wrapper.flushAll()))
      }

    }

  }
}

package object cache {
  def apply[Key, Val] = new KeyValueProvider[Key, Val]
}
