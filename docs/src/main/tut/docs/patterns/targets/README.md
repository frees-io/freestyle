---
layout: docs
title: Flexible Targets
permalink: /docs/patterns/targets/
---

# Flexible Targets

Scala does not currently have a cannonical `IO` monad as you may find in other FP languajes such as Haskell.
In Scala most projects use `scala.concurrent.Future`, `monix.eval.Task` or `fs2.Task` as the effect capturing monads to chain computations in most commmon applications.

Freestyle provides the following features to make sure you can choose the best target runtime for your applications.

1. First class support for `scala.concurrent.Future`, `monix.eval.Task` or `fs2.Task` with ready to go monad instances and optional nondeterministic instances to support parallelism.
2. A `Capture` typeclass that you can use as constrains on a generic `M[_]` to build interpreters that can work with any `M[_]` that can adhere to the `Capture` instance.
3. MTL style constrains on [`effects`](/docs/effects/) handlers that allows for max flexibility when interpreting to traditional Monad transformer stack such as `Kleisli[EitherT[Future, BizException, ?], Config, A]`.

In the documentation and examples below we will go over these main features and show you how you can build programs that are flexible and decoupled from interpretation.

## Runtime agnostic programs

Freestyle supports program declarations that are decoupled entirely from interpretation. This decoupling enables users to provide whatever concurrent or sync monad they favor in their applications.
Consider the following algebra and simple program definition.

```tut:book
import freestyle._
import freestyle.implicits._
import cats.implicits._

@free trait Validation[F[_]] {
  def minSize(s: String, n: Int): FreeS.Par[F, Boolean]
  def hasNumber(s: String): FreeS.Par[F, Boolean]
}

def validate(s: String)(implicit V : Validation[Validation.Op]): FreeS[Validation.Op, (Boolean, Boolean)] =
  (V.minSize(s, 5) |@| V.hasNumber(s)).tupled
```

In the program above we have defined two validation rules that are independent and that may be combined to determined if a given `s: String` conforms to those rules.

If we wanted to support a single runtime target for interpretation we could just define a `Handler[M[_]]` where M is our target runtime.
In our first example we will simply interpret to `scala.util.Try` for which a monad instance in cats already exists.

```tut:book
import scala.util.Try
import cats.instances.try_._

implicit val validationTryHandler: Validation.Handler[Try] = new Validation.Handler[Try] {
  override def minSize(s: String, n: Int): Try[Boolean] = Try(s.size >= n)
  override def hasNumber(s: String): Try[Boolean] = Try(s.exists(c => "0123456789".contains(c)))
}

validate("William Alvin Howard").exec[Try]
```

Similarly we can write handlers to support other monads such as `scala.concurrent.Future` and even take advantage of the nature of `minSize` and `hasNumber` which are independent operations not subject to monadic sequential computation but that can be expressed in the context of `Applicative`. With the `nondeterminism` instance that Freestyle provides for `scala.concurrent.Future`

```tut:book
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import freestyle.nondeterminism._

implicit val validationFutureHandler: Validation.Handler[Future] = new Validation.Handler[Future] {
  override def minSize(s: String, n: Int): Future[Boolean] = Future(s.size >= n)
  override def hasNumber(s: String): Future[Boolean] = Future(s.exists(c => "0123456789".contains(c)))
}

validate("Haskell Curry").exec[Future]
```

Following this approach we could keep on going and create handlers for every target `M[_]` we want our programs to be interpreted into.
This is fine if we know ahead of time where our program will run and how our users plan to use it but this is not always the case.
This is specially noticeable in the context of building libraries and DSLs. We, as library developers and maintainers, can't always anticipate
in which context our library may be used.

Freestyle provides abstractiones to help you write a single `Handler` that can satisfy multiple target runtimes.

## Abstracting over `Handler` targets

Freestyle provides a simple `Capture` typeclass which looks like the following:

```scala
@typeclass trait Capture[M[_]] {
  def capture[A](a: => A): M[A]
}
```

`Capture` allows to define a way to lift lazy computations into `M[_]`. If you can implement `Capture` for whatever target you are using and provide implicit evidence of its existence then you can write `Handler` instances that work in many contexts.

Freestyle provides `Capture` instances out of the box for `scala.concurrent.Future`, `scala.util.Try`, `cats.Id` as part of the core module.
Additionaly you can get `Capture` instances for `monix.eval.Task` in the _freestyle-monix_, `com.twitter.util.Future` in _freestyle-twitter-utils_ and `fs2.Task` in _freestyle-fs2_ respective modules.

We can easily write a `Handler` parametrized to `Capture` to support all of these runtimes like in the example below

```tut:book
implicit def validationCaptureHandler[M[_]](implicit C: Capture[M]): Validation.Handler[M] = new Validation.Handler[M] {
  override def minSize(s: String, n: Int): M[Boolean] = C.capture(s.size >= n)
  override def hasNumber(s: String): M[Boolean] = C.capture(s.exists(c => "0123456789".contains(c)))
}
```

We can now run our program to any of the `M[_]`s for which a `Capture` instance is found.
To demonstrate this capability we will implement a `Capture` instance for `cats.Eval`.

```tut:book
import cats.Eval

implicit val evalCapture: Capture[Eval] = new Capture[Eval] {
  override def capture[A](a: => A): Eval[A] = Eval.later(a)
}
```

Once there is evidence of such instance we can run our program to `Eval`. notice we did not have to implement a Handler and our generic `Capture` based one is automatically used when we provide evidence of `Capture[Eval]` through our implicit declaration.

```tut:book
validate("Edsger Wybe Dijkstra").exec[Eval]
```

Writting flexible programs that abstract away their target runtime is a flexible and simple technique and Freestyle provides all the machinery to make it a seamless experience.

to learn more about program interpretation visit the [Handlers](/docs/core/interpreters/) and [Parallelism](/docs/core/parallelism/) sections.
