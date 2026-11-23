package org.orbitalsuite.cora;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.orbitalsuite.cora.client.OrbitalDynamicsClient;
import org.orbitalsuite.orbital_dynamics.grpc.v1.PredictPassesRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.PredictPassesResponse;
import io.restassured.http.ContentType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.grpc.Status;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CoraResourceTest {
    @InjectMock
    OrbitalDynamicsClient orbital;

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

    @Test void omittedTickSecondsUsesTheL2Default() {
        when(orbital.predictPasses(any())).thenReturn(PredictPassesResponse.getDefaultInstance());

        given().contentType(ContentType.JSON)
                .body("""
                        {"catId":44883,"stationCode":"INPE-ALC",
                         "start":"2026-11-02T00:00:00Z","end":"2026-11-02T01:00:00Z"}
                        """)
                .post("/api/passes").then().statusCode(200);

        var request = ArgumentCaptor.forClass(PredictPassesRequest.class);
        verify(orbital).predictPasses(request.capture());
        org.junit.jupiter.api.Assertions.assertFalse(request.getValue().hasTickSeconds());
    }

    @Test void rejectsInvalidInputBeforeCallingL2() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"catId":0,"stationCode":" ",
                         "start":"2026-11-02T01:00:00Z","end":"2026-11-02T00:00:00Z"}
                        """)
                .post("/api/passes").then().statusCode(400);

        given().queryParam("pageSize", 501)
                .get("/api/satellites").then().statusCode(400);
    }

    @Test void mapsGrpcFailuresToStableHttpErrors() {
        when(orbital.predictPasses(any())).thenThrow(
                Status.UNAVAILABLE.withDescription("L2 offline").asRuntimeException());

        given().contentType(ContentType.JSON)
                .body("""
                        {"catId":44883,"stationCode":"INPE-ALC",
                         "start":"2026-11-02T00:00:00Z","end":"2026-11-02T01:00:00Z"}
                        """)
                .post("/api/passes").then().statusCode(503)
                .contentType(ContentType.JSON)
                .body("error", is("UNAVAILABLE"))
                .body("message", is("L2 offline"));
    }
}
