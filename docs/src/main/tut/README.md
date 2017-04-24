---
layout: fs-home
permalink: /
---
<section class="home-code" markdown="1">
<div class="container" markdown="1">
<div class="row" markdown="1">
<div class="col-md-6" markdown="1">
# Build purely functional applications and libraries
Build stack-safe purely functional applications and libraries that support parallel and sequential computations where declaration is decoupled from interpretation.
Freestyle encourages programs built atop [Free algebras](./docs/core/algebras) that are interpreted at the edge of your application ensuring effects are localized and performed in a controlled environment.
Applications built with Freestyle can be interpreted to any runtime semantics supported by the interpreter target type.
</div>
<div class="col-md-6" markdown="1">
```scala
import freestyle._

@free trait Database {
  def get(id: UserId): FS[User]
}

@free trait Cache {
  def get(id: UserId): FS[User]
}

@module trait Persistence {
  val database: Database
  val cache: Cache
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
# Automatic Dependency Injection
Freestyle includes all the implicit machinery necessary to achieve seamless dependency injection of [`@free`](./docs/core/algebras) and [`@module`](./docs/core/modules) Algebras.
Simply require any of your `@free` or `@module` trait as implicits where needed.
</div>
<div class="col-md-6 col-md-pull-6" markdown="1">
```scala
def storedUsers[F[_]]
    (userId: UserId)
    (implicit persistence: Persistence[F]): FreeS[F, (User, User)] = {
  import persistence._
  for {
    cachedUser <- cache.get(userId)
    persistentUser <- database.get(userId)
  } yield (cachedUser, persistentUser)
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
# Ready to use integrations
Freestyle ships with [ready to use algebras and convenient syntax extensions](./docs/integrations) covering most of the application concerns such as persistence, configuration, logging, etc.
In addition Freestyle includes commonly used FP [effects](./docs/effects) stack such as `option`, `error`, `reader`, `writer`, `state` based on the capabilities of
the target runtime interpreters.

</div>
<div class="col-md-6" markdown="1">
```scala
def loadUser[F[_]]
  (userId: UserId)
  (implicit 
    doobie: DoobieM[F], 
    logging: LoggingM[F]): FreeS[F, User] = {
    import doobie.implicits._
    for {
      user <- (sql"SELECT * FROM User WHERE userId = $userId"
                .query[User]
                .unique
                .liftFS[F])
      - <- logging.debug(s"Loaded User: ${user.userId}")
    } yield user
}
```
</div>
</div>
</div>
</section>
