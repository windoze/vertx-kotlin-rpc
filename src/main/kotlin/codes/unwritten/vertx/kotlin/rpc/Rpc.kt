@file:Suppress("RedundantVisibilityModifier")

package codes.unwritten.vertx.kotlin.rpc

import com.fasterxml.jackson.core.type.TypeReference
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.launch
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.full.callSuspend

fun Any.stringify(): String = Json.encode(this)

@Suppress("ArrayInDataClass")
data class RpcRequest(
        val service: String = "",
        val method: String = "",
        val args: Array<out Any>? = null
)

data class RpcResponse(
        val response: Any? = null
)

/**
 * RpcServerVerticle hosts all RPC service objects
 * @param channel Name of the eventbus channel
 */
public class RpcServerVerticle(private val channel: String) : CoroutineVerticle() {
    private interface RpcServer {
        suspend fun processRequest(request: RpcRequest): RpcResponse

        companion object {
            fun <T : Any> instance(impl: T): RpcServer {
                return object : RpcServer {
                    override suspend fun processRequest(request: RpcRequest): RpcResponse {
                        val ret = impl::class.members.first {
                            // TODO: Check signature to support overloading
                            it.name == request.method
                        }.callSuspend(impl, *(request.args ?: arrayOf()))
                        return RpcResponse(ret)
                    }
                }
            }
        }
    }

    private val services: HashMap<String, RpcServer> = hashMapOf()

    override suspend fun start() {
        launch(vertx.dispatcher()) {
            for (msg in vertx.eventBus().consumer<String>(channel).toChannel(vertx)) {
                // Start a new coroutine to handle the incoming request to support recursive call
                launch(vertx.dispatcher()) {
                    val req = Json.decodeValue(msg.body(), RpcRequest::class.java)
                    try {
                        msg.reply((services[req.service]?.processRequest(req)
                                ?: throw NoSuchElementException("Service ${req.service} not found")).stringify())
                    } catch (e: Throwable) {
                        msg.fail(1, e.message)
                    }
                }
            }
        }
    }

    /**
     * Register the service object
     * @param name Name of the service
     * @param impl Object which implements the service
     */
    fun <T : Any> register(name: String, impl: T): RpcServerVerticle {
        services[name] = RpcServer.instance(impl)
        return this
    }
}

/**
 * Dynamically create the service proxy object for the given interface
 * @param vertx Vertx instance
 * @param channel Name of the channel where RPC service listening
 * @param name Name of the service
 */
public inline fun <reified T : Any> getServiceProxy(vertx: Vertx, channel: String, name: String) =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            val lastArg = args?.lastOrNull()
            if (lastArg is Continuation<*>) {
                // The last argument of a suspend function is the Continuation object
                @Suppress("UNCHECKED_CAST") val cont = lastArg as Continuation<Any?>
                val argsButLast = args.take(args.size - 1)
                // Send request to the given channel on the event bus
                vertx.eventBus().send(channel, RpcRequest(name,
                        method.name,
                        argsButLast.toTypedArray()).stringify(),
                        Handler<AsyncResult<Message<String>>> { event ->
                            // Resume the suspended coroutine on reply
                            if (event?.succeeded() == true) {
                                cont.resume(Json.decodeValue(event.result().body(),
                                        object : TypeReference<RpcResponse>() {}).response)
                            } else {
                                cont.resumeWithException(event?.cause() ?: Exception("Unknown error"))
                            }
                        })
                // Suspend the coroutine to wait for the reply
                COROUTINE_SUSPENDED
            } else {
                // The function is not suspend
                null
            }
        } as T
