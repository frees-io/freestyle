---
layout: home
---

# Freestyle optionally includes
Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

```scala
import freestyle._
import freestyle.implicits._

@free trait Validation[F[_]] {
  def minSize(s: String, n: Int): FreeS.Par[F, Boolean]
  def hasNumber(s: String): FreeS.Par[F, Boolean]
}

@free trait Interaction[F[_]] {
  def tell(msg: String): FreeS[F, Unit]
  def ask(prompt: String): FreeS[F, String]
}
```

# Freestyle optionally includes
Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

```scala
import freestyle._
import freestyle.implicits._

@free trait Validation[F[_]] {
  def minSize(s: String, n: Int): FreeS.Par[F, Boolean]
  def hasNumber(s: String): FreeS.Par[F, Boolean]
}

@free trait Interaction[F[_]] {
  def tell(msg: String): FreeS[F, Unit]
  def ask(prompt: String): FreeS[F, String]
}
```

# Freestyle optionally includes
Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

```scala
import freestyle._
import freestyle.implicits._

@free trait Validation[F[_]] {
  def minSize(s: String, n: Int): FreeS.Par[F, Boolean]
  def hasNumber(s: String): FreeS.Par[F, Boolean]
}

@free trait Interaction[F[_]] {
  def tell(msg: String): FreeS[F, Unit]
  def ask(prompt: String): FreeS[F, String]
}
```
