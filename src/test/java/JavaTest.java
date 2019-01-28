import codes.unwritten.vertx.kotlin.rpc.RpcServerVerticle;
import codes.unwritten.vertx.kotlin.rpc.ServiceProxyFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class JavaTest {
    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    private Vertx vertx;

    @Before
    public void before() {
        vertx = rule.vertx();
    }

    interface AsyncHelloSvc {
        Future<String> hello(String name);
    }

    public class HelloSvcImpl {
        public String hello(String name) {
            return "Hello, " + name + "!";
        }
    }

    // Java API test
    @Test
    public void test1(TestContext context) {
        final String channel = "test1";

        Async async = context.async();
        vertx.deployVerticle((new RpcServerVerticle(channel)).register("hello", new HelloSvcImpl()));
        AsyncHelloSvc svc = ServiceProxyFactory.getAsyncServiceProxy(vertx, channel, "hello", AsyncHelloSvc.class);
        svc.hello("world").setHandler(ar -> {
            if (ar.succeeded()) {
                context.assertEquals("Hello, world!", ar.result());
            } else {
                context.assertTrue(false);
            }
        });
        async.complete();
    }
}
