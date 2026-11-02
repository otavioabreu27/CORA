package inpe.ccs.cora;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class CoraApplicationIT {
    @Test void exposesInfoAndHealth() {
        given().get("/api/info").then().statusCode(200)
                .body("application", is("cora"))
                .body("level", is("3"));
        given().get("/q/health/live").then().statusCode(200).body("status", is("UP"));
        given().get("/q/health/ready").then().statusCode(200).body("status", is("UP"));
    }
}
