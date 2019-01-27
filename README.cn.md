Vertx Kotlin RPC over EventBus
==============================

极简的Vertx/Kotlin RPC框架。


创建RPC service
-----------------------

```kotlin
import codes.unwritten.vertx.kotlin.rpc.RpcServerVerticle

//...

// 只要方法的签名匹配，服务可以不用实现用于定义接口的interface
class HelloSvcImpl {
    // 方法可以是suspend，也可以不是
    fun hello(name: String): String = "Hello, $name!"
}

// ...

vertx.deployVerticle(RpcServerVerticle("test-channel")
    .register("hello", HelloSvcImpl()))
```


调用RPC service
---------------------
```kotlin
import codes.unwritten.vertx.kotlin.rpc.getServiceProxy

// ...

// 服务接口定义
interface HelloSvc {
    // 成员函数必须是suspend，否则在调用的时候会抛出异常
    suspend fun hello(world: String): String
}

// ...

// 创建服务的Proxy对象
val svc: HelloSvc = getServiceProxy(vertx, "test-channel", "hello")
// 调用RPC服务
assertEqual("Hello, world!", svc.hello("world"))
```


TODO
----

* 支持方法重载
* RPC调用跟踪