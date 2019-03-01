import codes.unwritten.vertx.kotlin.rpc.HttpRequest
import codes.unwritten.vertx.kotlin.rpc.JsonRpcException
import codes.unwritten.vertx.kotlin.rpc.QueryParam
import codes.unwritten.vertx.kotlin.rpc.getHttpJsonRpcServiceProxy
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class KotlinHttpJsonTest {
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

    data class Comment(
            val postId: Int = 0,
            val id: Int = 0,
            val name: String = "",
            val email: String = "",
            val body: String = ""
    )

    interface DemoSvc {
        @HttpRequest(method = HttpMethod.GET, path = "comments")
        suspend fun getComments(postId: Int): List<Comment>
    }

    data class PostmanResponse(
            val args: Map<String, String> = mapOf(),
            val headers: Map<String, String> = mapOf(),
            val url: String = ""
    )

    interface PostmanSvc {
        @HttpRequest(method = HttpMethod.GET)
        suspend fun get(@QueryParam foo1: String, @QueryParam("foo2") arg2: String): PostmanResponse
    }

    @Test
    fun test1(context: TestContext) = r(context) {
        // Simple API call to a public REST API live demo
        val svc = getHttpJsonRpcServiceProxy<DemoSvc>(vertx, "https://jsonplaceholder.typicode.com/")
        context.assertTrue(svc.getComments(1).isNotEmpty())
    }

    @Test
    fun test2(context: TestContext) = r(context) {
        // Query params
        val svc = getHttpJsonRpcServiceProxy<PostmanSvc>(vertx, "https://postman-echo.com/")
        val ret = svc.get("bar1", "bar2")
        context.assertEquals("bar1", ret.args["foo1"])
        context.assertEquals("bar2", ret.args["foo2"])
        context.assertEquals("https://postman-echo.com/get?foo1=bar1&foo2=bar2", ret.url)
    }

    @Test
    fun test3(context: TestContext) = r(context) {
        // HTTP 404 test
        val svc = getHttpJsonRpcServiceProxy<PostmanSvc>(vertx, "https://postman-echo.com/xxx")
        try {
            // This call will trigger JsonRpcException
            svc.get("bar1", "bar2")
            context.assertTrue(false)
        } catch (e: JsonRpcException) {
            // The status code should be 404
            context.assertEquals(e.statusCode, 404)
        }
    }
}