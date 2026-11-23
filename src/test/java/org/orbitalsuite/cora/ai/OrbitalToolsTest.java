package org.orbitalsuite.cora.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.langchain4j.agent.tool.P;
import org.orbitalsuite.cora.client.OrbitalDynamicsClient;
import org.orbitalsuite.orbital_dynamics.grpc.v1.ElementsPage;
import org.orbitalsuite.orbital_dynamics.grpc.v1.ListSatellitesRequest;
import org.junit.jupiter.api.Test;

class OrbitalToolsTest {
    @Test void nameLookupDoesNotRequireNoradOrGroup() throws Exception {
        for (var parameter : OrbitalTools.class.getMethod("listSatellites", String.class, Long.class, Integer.class).getParameters()) {
            assertFalse(parameter.getAnnotation(P.class).required());
        }
        var client = new RecordingClient();
        var tools = new OrbitalTools();
        tools.orbital = client;
        tools.listSatellites("AMAZONIA", null, null);
        assertEquals("AMAZONIA", client.request.getFilter().getName());
        assertEquals(0, client.request.getFilter().getCatIdsCount());
        assertEquals(0, client.request.getFilter().getGroup());
        tools.listSatellites("AMAZONIA", 0L, 0);
        assertEquals(0, client.request.getFilter().getCatIdsCount());
    }

    @Test void rejectsIncompleteFootprintAndInvalidWindowsBeforeCallingL2() {
        var tools = new OrbitalTools();
        tools.orbital = new RecordingClient();

        assertThrows(IllegalArgumentException.class,
                () -> tools.getVisibilityFootprint("INPE-ALC", null, null, null, false, 36));
        assertThrows(IllegalArgumentException.class,
                () -> tools.propagate(44883, "2026-11-02T01:00:00Z",
                        "2026-11-02T00:00:00Z", 60, null, null));
    }

    private static class RecordingClient extends OrbitalDynamicsClient {
        ListSatellitesRequest request;
        @Override public ElementsPage listSatellites(ListSatellitesRequest request) {
            this.request = request;
            return ElementsPage.getDefaultInstance();
        }
    }
}
