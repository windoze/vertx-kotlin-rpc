Vertx Kotlin RPC over EventBus
==============================

A minimalist RPC framework for Vertx/Kotlin.


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


TODO
----

* Function overloading support.
* Call tracing and debugging.