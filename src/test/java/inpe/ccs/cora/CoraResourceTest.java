package inpe.ccs.cora;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.restassured.http.ContentType;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CoraResourceTest {
    @Test void exposesIdentityWithoutDatabaseOrGrpcCall() {
        given().get("/api/info").then().statusCode(200)
                .body("application", is("cora"))
                .body("level", is("3"));
    }

    @Test void chatIsUnavailableWhenAiIsDisabled() {
        given().contentType(ContentType.JSON)
                .body("{\"message\":\"teste\"}")
                .post("/api/chat").then().statusCode(503);
    }
}
