import codes.unwritten.vertx.kotlin.rpc.RpcServerVerticle
import codes.unwritten.vertx.kotlin.rpc.ServiceProxyFactory.getAsyncServiceProxy
import codes.unwritten.vertx.kotlin.rpc.ServiceProxyFactory.getServiceProxy
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class KotlinTest {
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

    interface HelloSvc {
        suspend fun hello(name: String): String
    }

    // Svc implementation w/o implementing interface and suspend
    @Test
    fun test1(context: TestContext) = r(context) {
        val channel = "test1"

        vertx.deployVerticle(RpcServerVerticle(channel).register("hello", object {
            @Suppress("unused")
            fun hello(name: String): String = "Hello, $name!"
        }))

        context.assertEquals("Hello, world!", getServiceProxy<HelloSvc>(vertx, channel, "hello").hello("world"))
    }

    // Svc implementation w/ implementing interface and suspend
    @Test
    fun test2(context: TestContext) = r(context) {
        val channel = "test2"

        vertx.deployVerticle(RpcServerVerticle(channel).register("hello", object : HelloSvc {
            override suspend fun hello(name: String): String = "Hello, $name!"
        }))
        context.assertEquals("Hello, world!", getServiceProxy<HelloSvc>(vertx, channel, "hello").hello("world"))
    }

    interface FooBarSvc {
        suspend fun foo(a: Int, b: String): String
        suspend fun bar(x: String): Int
    }

    // Svc with multiple methods
    @Test
    fun test3(context: TestContext) = r(context) {
        val channel = "test3"

        vertx.deployVerticle(RpcServerVerticle(channel).register("foobar", @Suppress("unused") object {
            fun foo(a: Int, b: String): String = "$a $b"
            fun bar(x: String): Int = x.toInt()
        }))
        with(getServiceProxy<FooBarSvc>(vertx, channel, "foobar")) {
            context.assertEquals("42 mouse", foo(42, "mouse"))
            context.assertEquals(42, bar("42"))
        }
    }

    // Cascading call
    @Test
    fun test4(context: TestContext) = r(context) {
        val channel = "test4"

        vertx.deployVerticle(RpcServerVerticle(channel)
                .register("hello", object {
                    @Suppress("unused")
                    fun hello(name: String): String = "Hello, $name!"
                })
                .register("foobar", object : FooBarSvc {
                    val svc: HelloSvc = getServiceProxy(vertx, channel, "hello")
                    override suspend fun foo(a: Int, b: String): String = "$a $b, ${svc.hello("world")}"
                    override suspend fun bar(x: String): Int = x.toInt()
                }))

        context.assertEquals("42 world, Hello, world!",
                getServiceProxy<FooBarSvc>(vertx, channel, "foobar").foo(42, "world"))
    }

    interface FibonacciSvc {
        suspend fun fibonacci(n: Long): Long
    }

    // Recursive call
    @Test
    fun test5(context: TestContext) = r(context) {
        val channel = "test5"

        vertx.deployVerticle(RpcServerVerticle(channel).register("fibonacci", object : FibonacciSvc {
            val self: FibonacciSvc = getServiceProxy(vertx, channel, "fibonacci")
            override suspend fun fibonacci(n: Long): Long {
                return if (n < 3) 1 else self.fibonacci(n - 1) + self.fibonacci(n - 2)
            }
        }))

        with(getServiceProxy<FibonacciSvc>(vertx, channel, "fibonacci")) {
            context.assertEquals(1L, fibonacci(1))
            context.assertEquals(1L, fibonacci(2))
            context.assertEquals(2L, fibonacci(3))
            context.assertEquals(3L, fibonacci(4))
            context.assertEquals(5L, fibonacci(5))
            context.assertEquals(8L, fibonacci(6))
        }
    }

    data class Sample(val a: Int = 0, val b: String = "")

    interface ComplexSvc {
        suspend fun f(v: Sample): String
    }

    // Svc with custom type argument
    @Test
    fun test6(context: TestContext) = r(context) {
        val channel = "test6"

        vertx.deployVerticle(RpcServerVerticle(channel).register("hello", object {
            @Suppress("unused")
            fun f(v: Sample): String = "${v.a}, ${v.b}!"
        }))

        context.assertEquals("42, world!", getServiceProxy<ComplexSvc>(vertx, channel, "hello").f(Sample(42, "world")))
    }

    interface NullableSvc {
        suspend fun f1(a: Int, b: String?): String

        suspend fun f2(a: Int): String?
    }

    // Svc with nullable argument and return value
    @Test
    fun test7(context: TestContext) = r(context) {
        val channel = "test7"

        vertx.deployVerticle(RpcServerVerticle(channel).register("hello", @Suppress("unused") object {
            fun f1(a: Int, b: String?): String {
                return "$a, ${b ?: "X"}"
            }

            fun f2(a: Int): String? {
                return if (a == 0) null else a.toString()
            }
        }))

        with(getServiceProxy<NullableSvc>(vertx, channel, "hello")) {
            context.assertEquals("42, world", f1(42, "world"))
            context.assertEquals("42, X", f1(42, null))
            context.assertEquals("10", f2(10))
            context.assertNull(f2(0))
        }
    }

    interface AsyncHelloSvc {
        fun hello(name: String): Future<String>
    }

    // Async svc proxy
    @Test
    fun test8(context: TestContext) = r(context) {
        val channel = "test8"

        vertx.deployVerticle(RpcServerVerticle(channel).register("hello", @Suppress("unused") object {
            fun hello(name: String): String {
                return "Hello, $name!"
            }
        }))

        with(getAsyncServiceProxy(vertx, channel, "hello", AsyncHelloSvc::class.java)) {
            context.assertEquals("Hello, world!", hello("world").await())
        }
    }
}
