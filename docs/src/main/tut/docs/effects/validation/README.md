---
layout: docs
title: Validation
permalink: /docs/effects/validation/
---

## ValidationM

The validation effect allows for the distinction between valid and invalid values in a program, accumulating the validation errors when executing it.

The `validation` effect, like [state](../state), supports parameterization to any type remaining type safe throughout the program declaration.

There needs to be implicit evidence of `MonadState[M[_], List[E]]`
for any runtime `M[_]` where `E` is the type of the validation error due to the constraints placed by this effect.

The validation effect comes with three basic operations `valid`, `invalid`, and `errors`. Apart from these, it includes a couple of combinators for accumulating errors: `fromEither` and `fromValidatedNel`.

```tut:silent
import freestyle._
import freestyle.implicits._
import freestyle.effects.validation
import cats.implicits._
import cats.data.State

sealed trait ValidationError
case class NotValid(explanation: String) extends ValidationError

val vl = validation[ValidationError]
import vl.implicits._

type ValidationResult[A] = State[List[ValidationError], A]
```

### valid

`valid` lifts a valid value to the program without accumulating any errors:

```tut:book
import cats.instances.list._

def programValid[F[_]: vl.ValidationM] =
  for {
    a <- FreeS.pure(1)
    b <- vl.ValidationM[F].valid(1)
    c <- FreeS.pure(1)
  } yield a + b + c

programValid[vl.ValidationM.Op].interpret[ValidationResult].runEmpty
```

### invalid

`invalid` accumulates a validation error:

```tut:book
def programInvalid[F[_]: vl.ValidationM] =
  for {
    a <- FreeS.pure(1)
    _ <- vl.ValidationM[F].invalid(NotValid("oh no"))
    b <- FreeS.pure(1)
  } yield a + b

programInvalid[vl.ValidationM.Op].interpret[ValidationResult].runEmpty
```

### errors

`errors` allows you to inspect the accumulated errors so far:

```tut:book
def programErrors[F[_]: vl.ValidationM] =
  for {
    _ <- vl.ValidationM[F].invalid(NotValid("oh no"))
    errs <- vl.ValidationM[F].errors
    _ <- vl.ValidationM[F].invalid(NotValid("this won't be in errs"))
  } yield errs

programErrors[vl.ValidationM.Op].interpret[ValidationResult].runEmpty
```

### fromEither

We can interleave `Either[ValidationError, ?]` values in the program. If they have errors on the left side they will be accumulated:

```tut:book
def programFromEither[F[_]: vl.ValidationM] =
  for {
    _ <- vl.ValidationM[F].fromEither(Left(NotValid("oh no")) : Either[ValidationError, Int])
	a <- vl.ValidationM[F].fromEither(Right(42) : Either[ValidationError, Int])
  } yield a

programFromEither[vl.ValidationM.Op].interpret[ValidationResult].runEmpty
```

### fromValidatedNel

We can interleave `ValidatedNel[ValidationError, ?]` values in the program. If they have errors in the invalid case they will be accumulated:

```tut:book
import cats.data.{Validated, ValidatedNel, NonEmptyList}

def programFromValidatedNel[F[_]: vl.ValidationM] =
  for {
    a <- vl.ValidationM[F].fromValidatedNel(
	   Validated.Valid(42)
	)
	_ <- vl.ValidationM[F].fromValidatedNel(
	    Validated.invalidNel[ValidationError, Unit](NotValid("oh no"))
    )
	_ <- vl.ValidationM[F].fromValidatedNel(
	    Validated.invalidNel[ValidationError, Unit](NotValid("another error!"))
    )
  } yield a

programFromValidatedNel[vl.ValidationM.Op].interpret[ValidationResult].runEmpty
```

### Syntax

By importing the validation effect implicits, a couple of methods are available for lifting valid and invalid values to our program: `liftValid` and `liftInvalid`.

```tut:book
def programSyntax[F[_]: vl.ValidationM] =
  for {
    a <- 42.liftValid
	_ <- NotValid("oh no").liftInvalid
	_ <- NotValid("another error!").liftInvalid
  } yield a

programSyntax[vl.ValidationM.Op].interpret[ValidationResult].runEmpty
```
