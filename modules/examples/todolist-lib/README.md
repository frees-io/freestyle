# TodoList lib

This example contains the definition of all algebras to manage todolists. It
provides the model, all persistence algebras and all service algebras.

It also provides an implementation for the persistence algebras using
[`doobie`](https://tpolecat.github.io/doobie/) with a `h2` database.

## Usages

This *library* has been used to implement two examples:

 - [`todolist-http-finch`](../todolist-http-finch): Application to manage todolists over http using [`finch`](https://finagle.github.io/finch/)
 - [`todolist-http-http4s`](../todolist-http-http4s): Application to manage todolists over http using [`http4s`](https://http4s.org/)
