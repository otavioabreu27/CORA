package org.orbitalsuite.cora;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InstrumentationTest {
    @ConfigProperty(name = "quarkus.application.name") String application;
    @ConfigProperty(name = "service.level") String level;

    @Test void exportsJvmHttpMetricsWithIdentity() {
        given().get("/api/info").then().statusCode(200);
        given().get("/q/metrics").then().statusCode(200)
                .body(containsString("jvm_memory_used_bytes"),
                        containsString("http_server_requests_seconds"),
                        containsString("application=\"" + application + "\""),
                        containsString("level=\"" + level + "\""));
    }

    @Test void exposesHealthChecks() {
        given().get("/q/health/live").then().statusCode(200);
        given().get("/q/health/ready").then().statusCode(200);
    }
}
