Vertx Kotlin RPC over EventBus
==============================
[![CircleCI](https://circleci.com/gh/windoze/vertx-kotlin-rpc.svg?style=svg)](https://circleci.com/gh/windoze/vertx-kotlin-rpc)
[![Download](https://api.bintray.com/packages/windoze/maven/vertx-kotlin-rpc/images/download.svg) ](https://bintray.com/windoze/maven/vertx-kotlin-rpc/_latestVersion)

A minimalist RPC framework for Vertx/Kotlin.

Getting Start
-------------

The artifact is hosted on jCenter, follow the [instruction](https://bintray.com/beta/#/bintray/jcenter)
to configure your building tool.

Maven:
```xml
<dependency>
    <groupId>codes.unwritten</groupId>
    <artifactId>vertx-kotlin-rpc</artifactId>
    <version>0.5</version>
    <type>pom</type>
</dependency>
```

Gradle:
```Groovy
compile 'codes.unwritten:vertx-kotlin-rpc:0.5'
```

To create a RPC service
-----------------------

```kotlin
import codes.unwritten.vertx.kotlin.rpc.RpcServerVerticle

//...

// Needs not to implement the service interface as long as the method signature matches
class HelloSvcImpl {
    // Method can be suspend or not
    fun hello(name: String): String = "Hello, $name!"
}

// ...

vertx.deployVerticle(RpcServerVerticle("test-channel")
    .register("hello", HelloSvcImpl()))
```

To call a RPC service
---------------------
```kotlin
import codes.unwritten.vertx.kotlin.rpc.getServiceProxy

// ...

interface HelloSvc {
    // Must be suspend, otherwise exceptions will be thrown on invocation.
    suspend fun hello(world: String): String
}

// ...

// Get the service proxy object
val svc: HelloSvc = getServiceProxy(vertx, "test-channel", "hello")
// Call the service
assertEqual("Hello, world!", svc.hello("world"))
```

To create a HTTP RPC service
----------------------------
```kotlin
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import codes.unwritten.vertx.kotlin.rpc.HttpRpcHandler

// ...
class SomeVerticle: CoroutineVerticle() {
    override suspend fun start() {
        // ...
        val router = Router.router(vertx)
        
        // Make sure body handler is enabled
        router.route().handler(BodyHandler.create())
        
        // Only POST method is supported
        router.post("/some-path").handler(HttpRpcHandler().register("hello", object {
            fun hello(name: String): String = "Hello, $name!"
        }))
        // Start HTTP server, etc.
        // ...
    }
}
```

To call a HTTP RPC service
--------------------------
```kotlin
import codes.unwritten.vertx.kotlin.rpc.getHttpServiceProxy

interface HelloSvc {
    // Must be suspend, otherwise exceptions will be thrown on invocation.
    suspend fun hello(name: String): String
}

// ...

// Get the service proxy object from a URL
val svc = getHttpServiceProxy<HelloSvc>(vertx, "http://127.0.0.1:8080/some-path", "hello")
// Call the service
assertEqual("Hello, world!", svc.hello("world"))

```

To call a JSON RPC service
--------------------------

```kotlin
import codes.unwritten.vertx.kotlin.rpc.HttpRequest
import codes.unwritten.vertx.kotlin.rpc.JsonRpcException
import codes.unwritten.vertx.kotlin.rpc.QueryParam
import codes.unwritten.vertx.kotlin.rpc.getHttpJsonRpcServiceProxy

interface DemoSvc {
    // Must be suspend, otherwise exceptions will be thrown on invocation.
    // HttpRequest annotation is used to customize mapping
    // Default method is POST and default path is the method name
    @HttpRequest(method = HttpMethod.GET, path = "comments")
    suspend fun getComments(postId: Int): List<Comment>
}

// ...

// Get the service proxy object from a URL
val svc = getHttpJsonRpcServiceProxy<PostmanSvc>(vertx, "https://postman-echo.com/")
// Call the service
context.assertTrue(svc.getComments(1).isNotEmpty())
```

Another example:

```
interface PostmanSvc {
    @HttpRequest(method = HttpMethod.GET)
    // QueryParam annotation indicates it is a query parameter instead of part of request body,
    // Also param name can be customized
    suspend fun get(@QueryParam foo1: String, @QueryParam("foo2") arg2: String): PostmanResponse
}
```

To create a RPC service in Java
-------------------------------
```java
import codes.unwritten.vertx.kotlin.rpc.RpcServerVerticle;

// ...

// Methods in the implementation class shall not return Future<T>, return T directly.
public class HelloSvcImpl {
    public String hello(String name) {
        return "Hello, " + name + "!";
    }
}

// ...

vertx.deployVerticle((new RpcServerVerticle("test-channel"))
    .register("hello", new HelloSvcImpl()));
```

To call a RPC service from Java
-------------------------------
Java doesn't have suspend functions, make sure every function in the service
interface returns `Future<T>` instead of `T`.
```Java
import io.vertx.core.Future;
import static codes.unwritten.vertx.kotlin.rpc.ServiceProxyFactory.getAsyncServiceProxy;

// ...

// Method must return Future<T> instead of T
interface AsyncHelloSvc {
    Future<String> hello(String world);
}

// ...

AsyncHelloSvc svc = getAsyncServiceProxy(vertx, "test-channel", "hello", AsyncHelloSvc.class);
svc.hello("world").setHandler(ar -> {
    if (ar.succeeded()) {
        assertEquals("Hello, world!", ar.result());
    } else {
        // Error handling
    }
});

```

To create a HTTP RPC service in Java
------------------------------------
TODO:


To call a HTTP RPC service from Java
------------------------------------

Java doesn't have suspend functions, make sure every function in the service
interface returns `Future<T>` instead of `T`.
```Java
import io.vertx.core.Future;
import static codes.unwritten.vertx.kotlin.rpc.AsyncServiceProxyFactory.getAsyncHttpServiceProxy;

// ...

// Method must return Future<T> instead of T
interface AsyncHelloSvc {
    Future<String> hello(String world);
}

// ...

AsyncHelloSvc svc = getAsyncHttpServiceProxy(vertx, "http://127.0.0.1:8080/some-path", "hello", AsyncHelloSvc.class);
svc.hello("world").setHandler(ar -> {
    if (ar.succeeded()) {
        assertEquals("Hello, world!", ar.result());
    } else {
        // Error handling
    }
});

```

Notes
-----

* JSON RPC use [Jackson](https://github.com/FasterXML/jackson) for serialization/deserialization. 
* All other arguments and returned values are serialized/deserialized by [Kryo](https://github.com/EsotericSoftware/kryo),
refer to its documentations for more details.
* Java Reflection API cannot retrieve function parameter names, which is required to build JSON object, so Java version of JSON RPC is unavailable.
Tell me if you have any ideas.

TODO
----

* Function overloading support.
* Call tracing and debugging.
* JSON RPC needs to support path parameters, which is used by many REST API.