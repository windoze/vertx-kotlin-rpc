Vertx Kotlin RPC over EventBus
==============================
[![CircleCI](https://circleci.com/gh/windoze/vertx-kotlin-rpc.svg?style=svg)](https://circleci.com/gh/windoze/vertx-kotlin-rpc)
[![Download](https://api.bintray.com/packages/windoze/maven/vertx-kotlin-rpc/images/download.svg) ](https://bintray.com/windoze/maven/vertx-kotlin-rpc/_latestVersion)

极简的Vertx/Kotlin RPC框架。


开始
----

Maven包已发布到jCenter，请先按照[说明](https://bintray.com/beta/#/bintray/jcenter)配置maven或gradle的设置。

Maven:
```xml
<dependency>
    <groupId>codes.unwritten</groupId>
    <artifactId>vertx-kotlin-rpc</artifactId>
    <version>0.6</version>
    <type>pom</type>
</dependency>
```

Gradle:
```Groovy
compile 'codes.unwritten:vertx-kotlin-rpc:0.6'
```
<hr>

在Kotlin中创建RPC service
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


在Kotlin中调用RPC service
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

在Kotlin中创建HTTP RPC service
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
        
        // 必须打开BodyHandler
        router.route().handler(BodyHandler.create())
        
        // 只支持POST method
        router.post("/some-path").handler(HttpRpcHandler().register("hello", object {
            fun hello(name: String): String = "Hello, $name!"
        }))
        // 启动HTTP server
        // ...
    }
}
```

在Kotlin中调用HTTP RPC service
--------------------------
```kotlin
import codes.unwritten.vertx.kotlin.rpc.getHttpServiceProxy

interface HelloSvc {
    // 成员函数必须是suspend，否则在调用的时候会抛出异常
    suspend fun hello(name: String): String
}

// ...

// 用指定的URL创建服务的Proxy对象
val svc = getHttpServiceProxy<HelloSvc>(vertx, "http://127.0.0.1:8080/some-path", "hello")
// 调用HTTP RPC服务
assertEqual("Hello, world!", svc.hello("world"))

```

在Kotlin中调用JSON RPC service
--------------------------

```kotlin
import codes.unwritten.vertx.kotlin.rpc.HttpRequest
import codes.unwritten.vertx.kotlin.rpc.JsonRpcException
import codes.unwritten.vertx.kotlin.rpc.QueryParam
import codes.unwritten.vertx.kotlin.rpc.getHttpJsonRpcServiceProxy

interface DemoSvc {
    // 成员函数必须是suspend，否则在调用的时候会抛出异常
    // HttpRequest annotation可以用了定制映射
    // 缺省的方法是POST，缺省路径是方法名
    @HttpRequest(method = HttpMethod.GET, path = "comments")
    suspend fun getComments(postId: Int): List<Comment>
}

// ...

// 用指定的URL创建服务的Proxy对象
val svc = getHttpJsonRpcServiceProxy<PostmanSvc>(vertx, "https://postman-echo.com/")
// 调用JSON RPC服务
context.assertTrue(svc.getComments(1).isNotEmpty())
```

Query Param示例：

```
interface DemoSvc {
    @HttpRequest(method = HttpMethod.POST, path="somepath")
    // QueryParam annotation表明这个参数是query parameter而不是body的一部分，也可以指定参数名称
    // foo3将会作为body的一部分被发往server
    suspend fun someMethod(@QueryParam foo1: String, @QueryParam("foo2") arg2: String, foo3: String): SomeResponse
}
```

在Java中创建RPC service
----------------------

```java
import codes.unwritten.vertx.kotlin.rpc.RpcServerVerticle;

// ...

// 实现类不返回Future<T>，直接返回T
public class HelloSvcImpl {
    public String hello(String name) {
        return "Hello, " + name + "!";
    }
}

// ...

vertx.deployVerticle((new RpcServerVerticle("test-channel"))
    .register("hello", new HelloSvcImpl()));
```

在Java中调用RPC service
-------------------------------
Java没有suspend函数，所以服务接口中的每个方法必须返回`Future<T>`而不是`T`。
```Java
import io.vertx.core.Future;
import static codes.unwritten.vertx.kotlin.rpc.ServiceProxyFactory.getAsyncServiceProxy;

// ...

// 方法必须返回Future<T>而不是T
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

在Java中创建HTTP RPC service
---------------------------
（略）


在Java中调用HTTP RPC service
---------------------------
Java没有suspend函数，所以服务接口中的每个方法必须返回`Future<T>`而不是`T`。
```Java
import io.vertx.core.Future;
import static codes.unwritten.vertx.kotlin.rpc.AsyncServiceProxyFactory.getAsyncHttpServiceProxy;

// ...

// 方法必须返回Future<T>而不是T
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

* JSON RPC使用[Jackson](https://github.com/FasterXML/jackson)进行序列化和反序列化。 
* 所有其它的参数和返回值都由[Kryo](https://github.com/EsotericSoftware/kryo)进行序列化和反序列化，请参阅文档以了解更多的细节。

TODO
----

* 支持方法重载
* RPC调用跟踪
* JSON RPC支持路径参数
