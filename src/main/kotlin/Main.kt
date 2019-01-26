import codes.unwritten.vertx.kotlin.rpc.RpcServerVerticle
import codes.unwritten.vertx.kotlin.rpc.getServiceProxy
import io.vertx.core.Vertx
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle


interface FooBarSvc {
    suspend fun foo(a: Int, b: String): String
    suspend fun bar(x: String): Int
}

interface HelloSvc {
    suspend fun hello(world: String): String
}

class TestRpcClientVerticle : CoroutineVerticle() {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    private val channel = "test"

    override suspend fun start() {
        val svc: FooBarSvc = getServiceProxy(vertx, channel, "foobar")
        log.info("Received string is '${svc.foo(42, "world")}'.")
        vertx.close()
    }
}

fun main() {
    val vertx = Vertx.vertx()

    // Implementation needs not to implement the service interface
    class HelloSvcImpl {
        // Method needs not to be suspend
        @Suppress("unused")
        fun hello(name: String): String = "Hello, $name!"
    }

    class FBSvcImpl : FooBarSvc {
        val svc: HelloSvc = getServiceProxy(vertx, "test", "hello")
        override suspend fun foo(a: Int, b: String): String = "$a $b, ${svc.hello("world")}"
        override suspend fun bar(x: String): Int = x.toInt()
    }

    vertx.deployVerticle(RpcServerVerticle("test")
            .register("hello", HelloSvcImpl())
            .register("foobar", FBSvcImpl()))
    vertx.deployVerticle(TestRpcClientVerticle())
}
