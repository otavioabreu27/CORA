package inpe.ccs.l1servicetemplate;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.grpc.ManagedChannelBuilder;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusIntegrationTest
class ExampleResourceIT extends ExampleResourceTest {
    @Test
    void servesGrpcAndExportsMetricsFromPackagedApplication() throws InterruptedException {
        var config = ConfigProvider.getConfig();
        int port = config.getOptionalValue("quarkus.grpc.server.test-port", Integer.class).orElse(9001);
        var channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        try {
            var reply = HelloGrpcGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .sayHello(HelloRequest.newBuilder().setName("native-test").build());
            assertEquals("Hello native-test!", reply.getMessage());
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }

        given().get("/hello").then().statusCode(200);
        String application = config.getValue("quarkus.application.name", String.class);
        given().get("/q/metrics").then().statusCode(200)
                .body(containsString("grpc_server_processing_duration_seconds"),
                        containsString("http_server_requests_seconds"), containsString("agroal_"),
                        containsString("application=\"" + application + "\""), containsString("level=\"1\""));
    }

    @Test
    void exposesHealthyPackagedApplication() {
        given().get("/q/health/live").then().statusCode(200).body("status", is("UP"));
        given().get("/q/health/ready").then().statusCode(200).body("status", is("UP"));
    }
}
