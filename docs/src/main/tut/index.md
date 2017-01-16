---
layout: fs-home
---
<section class="home-code" markdown="1">
<div class="container" markdown="1">
<div class="row" markdown="1">
<div class="col-md-6" markdown="1">
# Freestyle optionally includes
Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
</div>
<div class="col-md-6" markdown="1">
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
</div>
</div>
</div>
</section>
<section class="home-code" markdown="1">
<div class="container" markdown="1">
<div class="row" markdown="1">
<div class="col-md-6 col-md-push-6" markdown="1">
# Freestyle optionally includes
Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
</div>
<div class="col-md-6 col-md-pull-6" markdown="1">
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
</div>
</div>
</div>
</section>
<section class="home-code" markdown="1">
<div class="container" markdown="1">
<div class="row" markdown="1">
<div class="col-md-6" markdown="1">
# Freestyle optionally includes
Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
</div>
<div class="col-md-6" markdown="1">
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
</div>
</div>
</div>
</section>
