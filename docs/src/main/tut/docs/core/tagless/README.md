---
layout: docs
title: Tagless Final
permalink: /docs/core/tagless/
---

# Tagless Final

Freestyle supports additional encodings besides Free monads to achieve pure functional applications and libraries.

Tagless Final is among these and remains syntactically similar to `@free`.

To declare an algebra in the Tagless final encoding, you need to depend on `freestyle-tagless`.

## Dependencies

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```

[comment]: # (Start Replace)

For Scala.jvm:

```scala
libraryDependencies += "com.47deg" %% "freestyle-tagless" % "0.1.0"
```

For Scala.js:

```scala
libraryDependencies += "com.47deg" %%% "freestyle-tagless" % "0.1.0"
```

[comment]: # (End Replace)

## Declaration

Tagless final algebras are declared using the `@tagless` macro annotation.

```tut:book
import freestyle._
import freestyle.implicits._
import cats._
import cats.implicits._

@tagless trait Validation {
  def minSize(s: String, n: Int): FS[Boolean]
  def hasNumber(s: String): FS[Boolean]
}

@tagless trait Interaction {
  def tell(msg: String): FS[Unit]
  def ask(prompt: String): FS[String]
}
```

Once your `@tagless` algebras are defined, you can start building programs that rely upon implicit evidence of those algebras
being present, for the target runtime monad you are planning to interpret to.

```tut:book
def program[F[_]: Monad](implicit validation : Validation[F], interaction: Interaction[F]) = {
  import cats.implicits._

  for {
    userInput <- interaction.ask("Give me something with at least 3 chars and a number on it")
    valid <- (validation.minSize(userInput, 3) |@| validation.hasNumber(userInput)).map(_ && _)
    _ <- if (valid)
            interaction.tell("awesomesauce!") 
         else
            interaction.tell(s"$userInput is not valid")
  } yield ()
}
```

Note that unlike in `@free` `F[_]`, here it refers to the target runtime monad. This is to provide an allocation free model where your
ops are not being reified and then interpreted. This allocation step in Free monads is what allows them to be stack-safe.
The tagless final encoding with direct style syntax is as stack-safe as the target `F[_]` you are interpreting to.

## Interpretation

Once our `@tagless` algebras are defined, we can provide `Handler` instances in the same way we do with `@free`.

```tut:book
import scala.util.Try

implicit val validationHandler = new Validation.Handler[Try] {
  override def minSize(s: String, n: Int): Try[Boolean] = Try(s.size >= n)
  override def hasNumber(s: String): Try[Boolean] = Try(s.exists(c => "0123456789".contains(c)))
}

implicit val interactionHandler = new Interaction.Handler[Try] {
  override def tell(s: String): Try[Unit] = Try(println(s))
  override def ask(s: String): Try[String] = Try("This could have been user input 1")
}
```

At this point, we can run our pure programs at the edge of the world.

```tut:book
program[Try]
```

## Stack Safety

Freestyle provides two strategies to make `@tagless` encoded algebras stack safe.

### Interpreting to a stack safe monad

The handlers above are not stack safe because `Try` is not stack-safe. Luckily, we can rewrite our interpreters to 
interpret to `FreeS[Try, ?]` instead. This small penalty and a few extra allocations will make our programs stack safe.

```tut:book
implicit val validationHandlerStackSafe = new Validation.Handler[FreeS[Try, ?]] {
  override def minSize(s: String, n: Int): FreeS[Try, Boolean] = FreeS.liftFA(Try(s.size >= n))
  override def hasNumber(s: String): FreeS[Try, Boolean] = FreeS.liftFA(Try(s.exists(c => "0123456789".contains(c))))
}

implicit val interactionHandlerStackSafe = new Interaction.Handler[FreeS[Try, ?]] {
  override def tell(s: String): FreeS[Try, Unit] = FreeS.liftFA(Try(println(s)))
  override def ask(s: String): FreeS[Try, String] = FreeS.liftFA(Try("This could have been user input 1"))
}
```

We can now safely invoke our program in a stack safe way, running it to `Free[Try, ?]` first then to `Try`:

```tut.book
import cats.arrow.FunctionK

implicit def functionKIdentity[F[_]]: FunctionK[F, F] = FunctionK.id[F]

program[FreeS[Try, ?]].interpret[Try]
```

### Combining `@tagless` and `@free` algebras

Freestyle comes with built in support to compose `@free` and `@tagless` algebras.

For every `@tagless` algebra, there is also a free-based representation that is stack-safe by nature, and that can be used
to lift `@tagless` algebras to the context of application where `@free` and `@tagless` algebras coexist.

Let's redefine `program` to support `LoggingM` which is a `@free` defined algebra of logging operations:

```tut:book
import freestyle.logging._
import freestyle.loggingJVM.implicits._

def program[F[_]]
   (implicit log: LoggingM[F], 
             validation : Validation.StackSafe[F], 
             interaction: Interaction.StackSafe[F]) = {

  import cats.implicits._

  for {
    userInput <- interaction.ask("Give me something with at least 3 chars and a number on it")
    valid <- (validation.minSize(userInput, 3) |@| validation.hasNumber(userInput)).map(_ && _)
    _ <- if (valid)
            interaction.tell("awesomesauce!") 
         else
            interaction.tell(s"$userInput is not valid")
    _ <- log.debug("Program finished")
  } yield ()
}
```

Since `Validation` and `Interaction` were `@tagless` algebras, we need their `StackSafe` representation in order to combine
them with `@free` algebras.

### Interpreting combined `@tagless` and `@free` algebras

When combining `@tagless` and `@free` algebras, we need all algebras to be considered in the final Coproduct we are interpreting to.
We can simply use tagless's `.StackSafe` representation in modules so they are considered for the final Coproduct.

```tut:book
@module trait App {
  val interaction: Interaction.StackSafe
  val validation: Validation.StackSafe
  val log: LoggingM
}
```

Once all of our algebras are considered, we can execute our programs

```tut:book
program[App.Op].interpret[Try]
```