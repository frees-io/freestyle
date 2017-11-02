---
layout: docs
title: RPC
permalink: /docs/rpc
---

# Freestyle-RPC

[RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) atop Freestyle is `frees-rpc`, a Functional Programming wrapper of [gRPC](https://grpc.io/).

## What’s frees-rpc

`frees-rpc` brings the ability to combine RPC protocols, services and clients in your `Freestyle` program, thanks to [gRPC](https://grpc.io/). Yes, `frees-rpc` it's based on gRPC, offering an integration with it, with some differences when defining the protocols, as we’ll see later on, since `frees-rpc` follows the same philosophy than `Freestyle` core and it's macro-powered.

## About gRPC

> [gRPC](https://grpc.io/about/) is a modern open source high performance RPC framework that can run in any environment. It can efficiently connect services in and across data centers with pluggable support for load balancing, tracing, health checking and authentication. It is also applicable in last mile of distributed computing to connect devices, mobile applications and browsers to backend services.

## gRPC - Messages and Services

`gRPC` uses protocol buffers for serializing/deserializing structure data. Similarly as you do with [JSON](https://en.wikipedia.org/wiki/JSON) data, defining files `.json` extension, with protocol buffers you have to define proto files with `.proto` as extension.

In the example given in the [gRPC guide](https://grpc.io/docs/guides/), you might have a proto file like this:

```
message Person {
  string name = 1;
  int32 id = 2;
  bool has_ponycopter = 3;
}
```

Then, once you’ve specified your data structures, you can use the protocol buffer compiler `protoc` to generate data access classes in your preferred language(s) from your proto definition. In the Scala ecosystem, the most widely used is [ScalaPB](https://scalapb.github.io/).

Likewise you can define gRPC services in your proto files, with RPC method parameters and return types specified as protocol buffer messages:

```
// The greeter service definition.
service Greeter {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

// The request message containing the user's name.
message HelloRequest {
  string name = 1;
}

// The response message containing the greetings
message HelloReply {
  string message = 1;
}
```

Correspondingly, gRPC also uses protoc with a special gRPC plugin to generate code from your proto file for this Greeter RPC service. See [this ScalaPB section](https://scalapb.github.io/grpc.html) for a deeper explanation.

You can find more information about Protocol Buffers in the [Protocol Buffers documentation](https://developers.google.com/protocol-buffers/docs/overview).

## frees-rpc - Messages and Services

In the previous section we’ve seen an overview about how gRPC and ScalaPB offer for defining protocols and generating (compiling protocol buffers) code. Now, let’s see how frees-rpc offers the same in the Freestyle fashion, following the FP principles.

First things first, the main difference respect to gRPC is `frees-rpc doesn’t need .proto files, but it still uses protocol buffers, thanks to the [PBDirect](https://github.com/btlines/pbdirect) library, which allows to read and write Scala objects directly to Protobuf with no .proto file definitions. Therefore, in summary we have:

* Your protocols, both messages and services will reside in you scala files, together with your business-logic, using scala-meta annotations to set them up. We’ll see more details shortly.
* Instead of reading .proto files to set up the RPC services and messages, frees-rpc offers (as an optional feature) generating them, based on your protocols defined in your Scala code. This feature is offer to keep compatibility with other languages and systems out of Scala.

Let’s start defining how to define the Person message we saw in the previous section. Before get starting, these are the scala imports we would need:

```tut:silent
import freestyle._
import freestyle.rpc.protocol._
``` 

In this case, it’s quite simple since it’s just a Scala case class preceded by the `@message` annotation:


```tut:book
/**
  * Message Example.
  *
  * @param name Person name.
  * @param id Person Id.
  * @param has_ponycopter Has Ponycopter check.
  */
@message
case class Person(name: String, id: Int, has_ponycopter: Boolean)
```

By the same token, let’s see now how the `Greeter` service would be translated to `frees-rpc` style (in your `.scala` file):

```tut:book
@option(name = "java_package", value = "quickstart", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Quickstart", quote = true)
object protocols {

  /**
   * The request message containing the user's name.
   * @param name User's name.
   */
  @message
  case class HelloRequest(name: String)

  /**
   * The response message,
   * @param message Message containing the greetings.
   */
  @message
  case class HelloReply(message: String)

  @free
  @service
  trait Greeter {

    /**
     * The greeter service definition.
     *
     * @param request Say Hello Request.
     * @return HelloReply.
     */
    @rpc def sayHello(request: HelloRequest): FS[HelloReply]

  }
}
```

Naturally, the RPC services are grouped in a [@free algebra](http://frees.io/docs/core/algebras/). Hence, we are following one of the main principles of Freestyle, you only need to concentrate on the API that you want to be exposed as abstract smart constructors, without worrying how they will be implemented.

In addition, we are using a couple of additional annotations:

* `@service`: tags the `@free` algebra as RPC service, in order to derive server and client code (macro expansion). Important: first should go `@free` followed by `@service`, and not inversely.
* `rpc`: this annotation indicates the method is an RPC service.

We'll see more details about these and other annotations in the following sections.

# References

* [Freestyle](http://frees.io/).
* [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call)
* [gRPC](https://grpc.io/)
* [Protocol Buffers Docs](https://developers.google.com/protocol-buffers/docs/overview)
* [PBDirect](https://github.com/btlines/pbdirect)
