package inpe.ccs.l1servicetemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InstrumentationTest {
    @GrpcClient HelloGrpc helloGrpc;
    @Inject MyEntityMapper mapper;
    @ConfigProperty(name = "quarkus.application.name") String application;

    @Test void exportsJvmHttpGrpcAndPoolMetricsWithIdentity() {
        helloGrpc.sayHello(HelloRequest.newBuilder().setName("metrics-test").build())
                .await().atMost(Duration.ofSeconds(5));
        given().get("/hello").then().statusCode(200);
        given().get("/q/metrics").then().statusCode(200)
                .body(containsString("jvm_memory_used_bytes"), containsString("http_server_requests_seconds"),
                        containsString("grpc_server_processing_duration_seconds"), containsString("agroal_"),
                        containsString("application=\"" + application + "\""), containsString("level=\"1\""));
    }

    @Test void exposesHealthChecks() {
        given().get("/q/health/live").then().statusCode(200);
        given().get("/q/health/ready").then().statusCode(200);
    }

    @Test void mapsLombokAccessorsThroughGeneratedCdiMapper() {
        var entity = new MyEntity();
        entity.id = 42L;
        entity.setField("CCS");
        var response = mapper.toResponse(entity);
        assertEquals(42L, response.id());
        assertEquals("CCS", response.field());
        var copy = mapper.toEntity(response);
        assertNull(copy.id);
        assertEquals("CCS", copy.getField());
    }
}
