---
layout: docs
title: RPC
permalink: /docs/rpc
---

# Freestyle-RPC

[RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) atop **Freestyle** is `frees-rpc`, in other words `frees-rpc` is a Functional Programming wrapper of [gRPC](https://grpc.io/).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

  - [What’s frees-rpc](#whats-frees-rpc)
  - [About gRPC](#about-grpc)
  - [Messages and Services](#messages-and-services)
    - [gRPC](#grpc)
    - [frees-rpc](#frees-rpc)
  - [Service Methods](#service-methods)
  - [Generating a .proto file](#generating-a-proto-file)
    - [Plugin Installation](#plugin-installation)
    - [Plugin Settings](#plugin-settings)
    - [Generation with protoGen](#generation-with-protogen)
  - [RPC Service Implementations](#rpc-service-implementations)
    - [Server](#server)
    - [Server Runtime](#server-runtime)
      - [Execution Context](#execution-context)
      - [Runtime Implicits](#runtime-implicits)
      - [Server Bootstrap](#server-bootstrap)
- [References](#references)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## What’s frees-rpc

`frees-rpc` brings the ability to combine RPC protocols, services and clients in your `Freestyle` program, thanks to [gRPC](https://grpc.io/). Although it's fully integrated with gRPC, there are some differences when defining the protocols, as we’ll see later on, since `frees-rpc` follows the same philosophy that `Freestyle` core, being macro-powered.

## About gRPC

> [gRPC](https://grpc.io/about/) is a modern open source high performance RPC framework that can run in any environment. It can efficiently connect services in and across data centers with pluggable support for load balancing, tracing, health checking and authentication. It is also applicable in last mile of distributed computing to connect devices, mobile applications and browsers to backend services.

## Messages and Services

### gRPC

`gRPC` uses protocol buffers (protobuf from now on):

* As the Interface Definition Language (IDL) for describing both the service interface and the structure of the payload messages. It is possible to use other alternatives if desired. 
* For serializing/deserializing structure data, similarly as you do with [JSON](https://en.wikipedia.org/wiki/JSON) data, defining files `.json` extension, with protocol buffers you have to define proto files with `.proto` as extension.

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

### frees-rpc

In the previous section, we’ve seen an overview about what gRPC and ScalaPB offer for defining protocols and generating (compiling protocol buffers) code. Now, let’s see how frees-rpc offers the same in the Freestyle fashion, following the FP principles.

First things first, the main difference respect to gRPC is `frees-rpc doesn’t need `.proto` files, but it still uses protocol buffers, thanks to the [PBDirect](https://github.com/btlines/pbdirect) library, which allows to read and write Scala objects directly to Protobuf with no `.proto` file definitions. Therefore, in summary we have:

* Your protocols, both messages and services will reside in you scala files, together with your business-logic, using scala-meta annotations to set them up. We’ll see more details shortly.
* Instead of reading `.proto` files to set up the RPC services and messages, frees-rpc offers (as an optional feature) generating them, based on your protocols defined in your Scala code. This feature is offered to keep compatibility with other languages and systems out of Scala.

Let’s start seeing how to define the Person message that we saw previously. 
These are the scala imports we would need:

```tut:silent
import freestyle._
import freestyle.rpc.protocol._
``` 

Person definition would be defined as follows:

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

As we can see, it’s quite simple since it’s just a Scala case class preceded by the `@message` annotation:

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

* `@service`: it tags the `@free` algebra as RPC service, in order to derive server and client code (macro expansion). **Important**: `@free` annotation should go first, followed by `@service` annotation, and not inversely.
* `rpc`: this annotation indicates the method is an RPC service.

We'll see more details about these and other annotations in the following sections.

## Service Methods

As `gRPC`, `frees-rpc` allows you to define four kinds of service method:

* **Unary RPC**: simplest way of communication, one request/ one response.
* **Server streaming RPC**: similar to the unary, but in this case the server will send back a stream of responses for the client request.
* **Client streaming RPC**: in this case is the client who sends a stream of requests. The server will respond with a single response.
* **Bidirectional streaming RPC**: it would be a mix of server and client streaming, since both side will be sending a stream of data.

Let's complete our protocol's example with this four kinds of service methods:

```tut:book
@option(name = "java_package", value = "quickstart", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Quickstart", quote = true)
object service {

  import monix.reactive.Observable

  @message
  case class HelloRequest(greeting: String)

  @message
  case class HelloResponse(reply: String)

  @free
  @service
  trait Greeter {

    /**
     * Unary RPCs where the client sends a single request to the server and gets a single response back,
     * just like a normal function call.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Client Request.
     * @return Server Response.
     */
    @rpc
    def sayHello(request: HelloRequest): FS[HelloResponse]

    /**
     * Server streaming RPCs where the client sends a request to the server and gets a stream to read a
     * sequence of messages back. The client reads from the returned stream until there are no more messages.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Client Request.
     * @return Stream of server responses.
     */
    @rpc
    @stream[ResponseStreaming.type]
    def lotsOfReplies(request: HelloRequest): FS[Observable[HelloResponse]]

    /**
     * Client streaming RPCs where the client writes a sequence of messages and sends them to the server,
     * again using a provided stream. Once the client has finished writing the messages, it waits for
     * the server to read them and return its response.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Stream of requests.
     * @return Single Server Response.
     */
    @rpc
    @stream[RequestStreaming.type]
    def lotsOfGreetings(request: Observable[HelloRequest]): FS[HelloResponse]

    /**
     * Bidirectional streaming RPCs where both sides send a sequence of messages using a read-write stream.
     * The two streams operate independently, so clients and servers can read and write in whatever order
     * they like: for example, the server could wait to receive all the client messages before writing its
     * responses, or it could alternately read a message then write a message, or some other combination of
     * reads and writes. The order of messages in each stream is preserved.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Stream of requests.
     * @return Stream of server responses.
     */
    @rpc
    @stream[BidirectionalStreaming.type]
    def bidiHello(request: Observable[HelloRequest]): FS[Observable[HelloResponse]]

  }

}
```

The code might be explanatory by itself but let's review the different services one by one:

* `sayHello`: unary RPC, only `@rpc` annotation would be needed in this case.
* `lotsOfReplies `: Server streaming RPC, where `@rpc` and `@stream` annotations are needed here. However, there are three different types of streaming, that is specified by the type parameter required in the `@stream` annotation, `@stream[ResponseStreaming.type]` in this particular definition.
* `lotsOfGreetings `: Client streaming RPC, `@rpc` should be scorted by the `@stream[RequestStreaming.type]` annotation.
* `bidiHello `: Bidirectional streaming RPC, where `@rpc` is accompanied by the `@stream[BidirectionalStreaming.type]` annotation.

**Note**: in `frees-rpc`, the streaming features have been implemented with `monix.reactive.Observable`, see [Monix Docs](https://monix.io/docs/2x/reactive/observable.html) for a wide explanation. These monix extensions have implemented atop the [gRPC Java API](https://grpc.io/grpc-java/javadoc/) and the `StreamObserver` interface.

So far so good, no much code, no business logic, just a protocol definition with Scala annotations.

## Generating a .proto file

Before entering in implementation details, we mentioned that `frees-rpc` ecosystem brings the ability to generate `.proto` files from the Scala definition, in order to keep compatibility with other languages and systems out of Scala.

This responsibility relies on [sbt-freestyle-protogen](https://github.com/frees-io/sbt-freestyle-protogen), an Sbt plugin to generate `.proto` files from the frees-rpc service definitions.

### Plugin Installation

Add the following line to _project/plugins.sbt_:

```scala
addSbtPlugin("io.frees" % "sbt-frees-protogen" % "0.0.13")
```

### Plugin Settings

There are a couple of settings key that can be configured according to the needs:

* `protoGenSourceDir`: the Scala source directory, where your `frees-rpc` definitions are placed. By default: `baseDirectory.value / "src" / "main" / "scala"`.
* `protoGenTargetDir `: The Protocol Buffers target directory, where the `protoGen` task will write the `.proto` files, based on frees-rpc service definitions. By default: `baseDirectory.value / "src" / "main" / "proto"`.

Directories must exist, otherwise the `protoGen` task will fail.

### Generation with protoGen

At this moment, each time you want to update your `.proto` files from the scala definition, you have to run the following sbt task:

```bash
sbt protoGen
```

Using the example above, the result would be (`/src/main/proto/service.proto`, in case the scala file is named as `service.scala`):

```
// This file has been automatically generated for use by
// sbt-frees-protogen plugin, from freestyle-rpc service definitions

syntax = "proto3";

option java_package = "quickstart";
option java_multiple_files = true;
option java_outer_classname = "Quickstart";
           
message HelloRequest {
   string greeting = 1;
}
           
message HelloResponse {
   string reply = 1;
}
           
service Greeter {
   rpc sayHello (HelloRequest) returns (HelloResponse) {}
   rpc lotsOfReplies (HelloRequest) returns (stream HelloResponse) {}
   rpc lotsOfGreetings (stream HelloRequest) returns (HelloResponse) {}
   rpc bidiHello (stream HelloRequest) returns (stream HelloResponse) {}
}
```

## RPC Service Implementations

In this section we are going to see how to complete our quickstart example. We'll se both sides, server and client.

### Server

Predictably, generating the server code it's just implement a service [Handler](http://frees.io/docs/core/interpreters/).

Next, our dummy Greeter server implementation:

```tut:book
import cats.~>
import freestyle.Capture
import monix.eval.Task
import monix.reactive.Observable
import service._

class ServiceHandler[F[_]](implicit C: Capture[F], T2F: Task ~> F) extends Greeter.Handler[F] {

  private[this] val dummyObservableResponse: Observable[HelloResponse] =
    Observable.fromIterable(1 to 5 map (i => HelloResponse(s"Reply $i")))

  override def sayHello(request: HelloRequest): F[HelloResponse] =
    C.capture(HelloResponse(reply = "Good bye!"))

  override def lotsOfReplies(request: HelloRequest): F[Observable[HelloResponse]] =
    C.capture(dummyObservableResponse)

  override def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse] = T2F {
    request
      .foldLeftL((0, HelloResponse(""))) {
        case ((i, response), currentRequest) =>
          val currentReply: String = response.reply
          (
            i + 1,
            response.copy(
              reply = s"$currentReply\nRequest ${currentRequest.greeting} -> Response: Reply $i"))
      }
      .map(_._2)
  }

  override def bidiHello(request: Observable[HelloRequest]): F[Observable[HelloResponse]] =
    C.capture {
      request
        .flatMap { request: HelloRequest =>
          println(s"Saving $request...")
          dummyObservableResponse
        }
        .onErrorHandle { e =>
          throw e
        }
    }
}
```

That's all, here we have exposed a potential implementation in the server side.

### Server Runtime

As you can see, the generic handler above requires `F` as type parameter, which it corresponds with our target `Monad` when interpreting our program. In this section, we are going to satisfy all the runtime requirements, in order to make our server runnable.

#### Execution Context

In `frees-rpc` programs we'll need, at least an implicit evidence related to the Monix executed context: `monix.execution.Scheduler`. 

> The `monix.execution.Scheduler` is inspired by `ReactiveX`, being an enhanced Scala `ExecutionContext` and also a replacement for Java’s `ScheduledExecutorService`, but also for Javascript’s `setTimeout`.

In this case, for our example, we need to provide a `scala.concurrent.ExecutionContext` implicit evidence, because we'll interpret our program to `scala.concurrent.Future`:

```tut:book
import scala.concurrent.ExecutionContext
import monix.execution.Scheduler

trait CommonRuntime {

  implicit val EC: ExecutionContext = ExecutionContext.Implicits.global
  implicit val S: Scheduler         = monix.execution.Scheduler.Implicits.global

}
```

As aside note, `CommonRuntime` will be also used later on for the client program.

#### Runtime Implicits

Now, we need to provide implicitly two things:

* A runtime interpreter of our `ServiceHandler` tied to an specific type. In our case we'll use `scala.concurrent.Future`.
* A Natural Transformation implicit evidence of `GrpcServer.Op ~> F` where `F` would be your target monad, in our example the implicit evidence would be: `GrpcServer.Op ~> Future`. To tackle this we have to consider few things:
	* First of all, we need to decide the rpc port where server will bootstrap.
	* Then, we need to register the set of configurations we want to add to our gRPC server, like the fact of adding our `Greeter` service. All these configurations are aggregated in a `List[GrpcConfig]`. Later on, an internal builder will build the final server based on this list. The full available list of settings are exposed in [this file](https://github.com/frees-io/freestyle-rpc/blob/master/rpc/src/main/scala/server/GrpcConfig.scala).
	* Finally, we need to compose:
		* A runtime interpreter of `freestyle.rpc.server.handlers.GrpcServerHandler`, which is another handler encharged of managing the server.
		* With another runtime interpreter of `freestyle.rpc.server.GrpcKInterpreter`, who will configure the server.

In summary, this would be the result:

```tut:book
import cats.~>
import cats.implicits._
import freestyle.implicits._
import freestyle.async.implicits._
import freestyle.rpc.server._
import freestyle.rpc.server.handlers._
import freestyle.rpc.server.implicits._

import scala.concurrent.Future

object gserver {

  trait Implicits extends CommonRuntime {

    implicit val greeterServiceHandler: Greeter.Handler[Future] = new ServiceHandler[Future]

    val grpcConfigs: List[GrpcConfig] = List(
      AddService(Greeter.bindService[Greeter.Op, Future])
    )

    implicit val grpcServerHandler: GrpcServer.Op ~> Future =
      new GrpcServerHandler[Future] andThen
        new GrpcKInterpreter[Future](ServerW(8080, grpcConfigs).server)
  }

  object implicits extends Implicits

}
```

Few additional notes:

* Server will bootstrap on port `8080`.
* `Greeter.bindService` is auto-derived method which will create the binding service for gRPC, behind the scenes. It expect two type parameters, `F[_]` and `M[_]`.
	* `F[_]` would be our algebra, which matches with our Greeter service definition.
	* `M[_]`, the target monad, in our example: `scala.concurrent.Future`.

#### Server Bootstrap

What else is needed? We just need to define a `main` method and:

```scala
import cats.implicits._
import freestyle.rpc.server.GrpcServerApp
import freestyle.rpc.server.implicits._
import gserver.implicits._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object RPCServer {

  def main(args: Array[String]): Unit =
    Await.result(server[GrpcServerApp.Op].bootstrapM[Future], Duration.Inf)

}
```

Once all the runtime requirements are in place (`import gserver.implicits._`), we only have to write the previous piece of code, which would be pretty much the same in all the cases (except if your target Monad is different from `Future`).

### Client



# References

* [Freestyle](http://frees.io/)
* [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call)
* [gRPC](https://grpc.io/)
* [Protocol Buffers Docs](https://developers.google.com/protocol-buffers/docs/overview)
* [PBDirect](https://github.com/btlines/pbdirect)
* [Monix](https://monix.io)
* [gRPC Java API](https://grpc.io/grpc-java/javadoc/)
