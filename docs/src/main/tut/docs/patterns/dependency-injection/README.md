---
layout: docs
title: Dependency Injection
permalink: /docs/patterns/dependency-injection/
---

## Dependency Injection

Freestyle makes it easy to create programs where the business logic is separated from specific implementation concerns like error handling, configuration, (asynchronous) communication with (remote) services, etc. The business logic is written using the operations from one or more algebras and every algebra by itself is implemented in a handler (interpreter).

As the business logic is built by glueing together simple algebra operations, it is just a pure description and only at the edge of the program (a main method, a http route, a UI handler, etc) a handler is needed, that can translate the description to a more concrete data type. These pure descriptions are also easy to test by supplying an appropriate test handler (eg a simpler one using just `Id`).

If you are using more than one algebra, freestyle makes it easy to execute or translate your resulting program because it will provide a handler for your combined algebra by combining all the individual handlers (see the [modules](/docs/core/modules/)) section).

Pairing up the right interpreter when you want to execute a program happens at compile time, so compiling your projects using freestyle may take a bit longer, but you don't need to worry about something going wrong at runtime.


### Runtime Dependency using Reader

For runtime dependency injection you can use a `Reader` data type.

Freestyle has a [`reader` effect algebra](/docs/effects/#reader), that is used in [the target stack example](/docs/stack/). However there are also cases where you don't want to put this dependency on some environment explicitly in your algebra and then you can use `Reader` or `ReaderT` (`Kleisli`) as part of you target type.

Imagine you are writing a `Persistence` algebra, you would not want to pollute it with implementation details like which connection pool is used to execute the persistence operations.
