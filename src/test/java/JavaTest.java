import codes.unwritten.vertx.kotlin.rpc.RpcServerVerticle;

import static codes.unwritten.vertx.kotlin.rpc.ServiceProxyFactory.getAsyncServiceProxy;

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
        AsyncHelloSvc svc = getAsyncServiceProxy(vertx, channel, "hello", AsyncHelloSvc.class);
        svc.hello("world").setHandler(ar -> {
            if (ar.succeeded()) {
                context.assertEquals("Hello, world!", ar.result());
            } else {
                context.assertTrue(false);
            }
        });
        async.complete();
    }

    interface AsyncNullableSvc {
        Future<String> f1(int a, String b);

        Future<String> f2(int a);
    }

    public class NullableSvcImpl {
        public String f1(int a, String b) {
            return a + ", " + ((b == null) ? "X" : b);
        }

        public String f2(int a) {
            if (a == 0) return null;
            return Integer.toString(a);
        }
    }

    // Nullable argument and return value
    @Test
    public void test2(TestContext context) {
        final String channel = "test2";

        try {
            Async async = context.async();
            vertx.deployVerticle((new RpcServerVerticle(channel)).register("hello", new NullableSvcImpl()));
            AsyncNullableSvc svc = getAsyncServiceProxy(vertx, channel, "hello", AsyncNullableSvc.class);
            svc.f1(42, "world").setHandler(ar -> {
                if (ar.succeeded()) {
                    context.assertEquals("42, world", ar.result());
                } else {
                    context.assertTrue(false);
                }
            });

            svc.f1(42, null).setHandler(ar -> {
                if (ar.succeeded()) {
                    context.assertEquals("42, X", ar.result());
                } else {
                    context.assertTrue(false);
                }
            });

            svc.f2(42).setHandler(ar -> {
                if (ar.succeeded()) {
                    context.assertEquals("42", ar.result());
                } else {
                    context.assertTrue(false);
                }
            });

            svc.f2(0).setHandler(ar -> {
                if (ar.succeeded()) {
                    context.assertNull(ar.result());
                } else {
                    context.assertTrue(false);
                }
            });

            async.complete();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
