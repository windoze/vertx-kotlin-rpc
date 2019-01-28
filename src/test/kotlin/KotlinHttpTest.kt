import codes.unwritten.vertx.kotlin.rpc.HttpRpcHandler
import codes.unwritten.vertx.kotlin.rpc.getHttpServiceProxy
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class KotlinHttpTest {
    @Rule
    @JvmField
    var rule = RunTestOnContext()

    private lateinit var vertx: Vertx

    @Before
    fun before() {
        vertx = rule.vertx()
    }

    private fun r(context: TestContext, block: suspend () -> Unit) {
        GlobalScope.launch(vertx.dispatcher()) {
            with(context.async()) {
                block()
                complete()
            }
        }
    }

    @Suppress("PrivatePropertyName")
    private val BASE_PORT: Int = 20000

    class TestHttpVerticle(private val port: Int, private val handler: HttpRpcHandler, private val path: String = "/rpc") : CoroutineVerticle() {
        override suspend fun start() {
            val server = vertx.createHttpServer()
            val router = Router.router(vertx)
            router.route().handler(BodyHandler.create())
            router.post(path).handler(handler)
            server.requestHandler(router).listen(port)
        }
    }

    interface HelloSvc {
        suspend fun hello(name: String): String
    }

    // Svc implementation w/o implementing interface and suspend
    @Test
    fun test1(context: TestContext) = r(context) {
        val port = BASE_PORT + 1

        awaitResult<String> { vertx.deployVerticle(TestHttpVerticle(port, HttpRpcHandler(vertx).register("hello", object {
            @Suppress("unused")
            fun hello(name: String) = "Hello, $name!"
        })), it) }

        val svc = getHttpServiceProxy<HelloSvc>(vertx, "http://127.0.0.1:$port/rpc", "hello")

        context.assertEquals("Hello, world!", svc.hello("world"))
    }

    // Svc implementation w/ implementing interface and suspend
    @Test
    fun test2(context: TestContext) = r(context) {
        val port = BASE_PORT + 2

        awaitResult<String> { vertx.deployVerticle(TestHttpVerticle(port, HttpRpcHandler(vertx).register("hello", object : HelloSvc {
            override suspend fun hello(name: String) = "Hello, $name!"
        })), it) }

        val svc = getHttpServiceProxy<HelloSvc>(vertx, "http://127.0.0.1:$port/rpc", "hello")

        context.assertEquals("Hello, world!", svc.hello("world"))
    }

    interface FibonacciSvc {
        suspend fun fibonacci(n: Long): Long
    }

    // Recursive call
    @Test
    fun test5(context: TestContext) = r(context) {
        val port = BASE_PORT + 5

        awaitResult<String> { vertx.deployVerticle(TestHttpVerticle(port, HttpRpcHandler(vertx).register("fibonacci", object : FibonacciSvc {
            val self: FibonacciSvc = getHttpServiceProxy(vertx, "http://127.0.0.1:$port/rpc", "fibonacci")
            override suspend fun fibonacci(n: Long): Long {
                return if (n < 3) 1 else self.fibonacci(n - 1) + self.fibonacci(n - 2)
            }
        })), it) }

        with(getHttpServiceProxy<FibonacciSvc>(vertx, "http://127.0.0.1:$port/rpc", "fibonacci")) {
            context.assertEquals(1L, fibonacci(1))
            context.assertEquals(1L, fibonacci(2))
            context.assertEquals(2L, fibonacci(3))
            context.assertEquals(3L, fibonacci(4))
            context.assertEquals(5L, fibonacci(5))
            context.assertEquals(8L, fibonacci(6))
        }
    }
}