---
layout: docs
title: Effects
permalink: /docs/effects/
---

# Effects

Freestyle comes with a built-in list of ready-to-use effects modeled as `@free` algebras contained in the `freestyle-effects` module. The current release of `freestyle-effects` supports Scala.jvm and Scala.js.

[comment]: # (Start Replace)

For Scala.jvm:

```scala
libraryDependencies += "com.47deg" %% "freestyle-effects" % "0.1.0"
```

For Scala.js:

```scala
libraryDependencies += "com.47deg" %%% "freestyle-effects" % "0.1.0"
```

[comment]: # (End Replace)

If you are missing an effect from the following list please [create an issue](https://github.com/47deg/freestyle/issues/new)
so it can be considered for future releases.

- [error](./error)
- [either](./either)
- [option](./option)
- [reader](./reader)
- [writer](./writer)
- [state](./state)
- [traverse](./traverse)
- [validation](./validation)
