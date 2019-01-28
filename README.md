Vertx Kotlin RPC over EventBus
==============================
[![CircleCI](https://circleci.com/gh/windoze/vertx-kotlin-rpc.svg?style=svg)](https://circleci.com/gh/windoze/vertx-kotlin-rpc)
[ ![Download](https://api.bintray.com/packages/windoze/maven/vertx-kotlin-rpc/images/download.svg) ](https://bintray.com/windoze/maven/vertx-kotlin-rpc/_latestVersion)

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
    <version>0.3</version>
    <type>pom</type>
</dependency>
```

Gradle:
```Groovy
compile 'codes.unwritten:vertx-kotlin-rpc:0.3'
```

The testing version is published to [Bintray/windoze](https://bintray.com/beta/#/windoze/maven/vertx-kotlin-rpc).

Maven:
```xml
<dependency>
    <groupId>codes.unwritten</groupId>
    <artifactId>vertx-kotlin-rpc</artifactId>
    <version>0.4</version>
    <type>pom</type>
</dependency>
```

Gradle:
```Groovy
compile 'codes.unwritten:vertx-kotlin-rpc:0.4'
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
import codes.unwritten.vertx.kotlin.rpc.ServiceProxyFactory;

// ...

// Method must return Future<T> instead of T
interface AsyncHelloSvc {
    Future<String> hello(String world);
};

// ...

AsyncHelloSvc svc = ServiceProxyFactory.getAsyncServiceProxy(vertx, "test-channel", "hello", AsyncHelloSvc.class);
svc.hello("world").setHandler(ar -> {
    if (ar.succeeded()) {
        assertEquals("Hello, world!", ar.result());
    } else {
        // Error handling
    }
});

```


Changelog
---------
* 0.4: Java API

TODO
----

* Function overloading support.
* Call tracing and debugging.