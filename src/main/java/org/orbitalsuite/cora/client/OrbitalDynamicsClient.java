package org.orbitalsuite.cora.client;

import java.time.Duration;

import org.orbitalsuite.orbital_dynamics.grpc.v1.Elements;
import org.orbitalsuite.orbital_dynamics.grpc.v1.ElementsPage;
import org.orbitalsuite.orbital_dynamics.grpc.v1.GetVisibilityFootprintRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.ListSatellitesRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.ListStationsRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.OrbitalDynamics;
import org.orbitalsuite.orbital_dynamics.grpc.v1.PredictPassesRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.PredictPassesResponse;
import org.orbitalsuite.orbital_dynamics.grpc.v1.PropagateRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.PropagateResponse;
import org.orbitalsuite.orbital_dynamics.grpc.v1.StationPage;
import org.orbitalsuite.orbital_dynamics.grpc.v1.VisibilityFootprint;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;

/** Fachada bloqueante do client gRPC do L2 orbital-dynamics. */
@ApplicationScoped
public class OrbitalDynamicsClient {

    private static final Duration DEADLINE = Duration.ofSeconds(15);

    @GrpcClient("orbital-dynamics")
    OrbitalDynamics client;

    public Elements getElements(long catId) {
        var request = org.orbitalsuite.orbital_dynamics.grpc.v1.GetElementsRequest.newBuilder()
                .setObject(org.orbitalsuite.orbital_dynamics.grpc.v1.ObjectRef.newBuilder()
                        .setCollection(org.orbitalsuite.orbital_dynamics.grpc.v1.Collection.SATELLITES)
                        .setCatId(catId))
                .build();
        return client.getElements(request).await().atMost(DEADLINE);
    }

    public ElementsPage listSatellites(ListSatellitesRequest request) {
        return client.listSatellites(request).await().atMost(DEADLINE);
    }

    public StationPage listStations(ListStationsRequest request) {
        return client.listStations(request).await().atMost(DEADLINE);
    }

    public PredictPassesResponse predictPasses(PredictPassesRequest request) {
        return client.predictPasses(request).await().atMost(DEADLINE);
    }

    public PropagateResponse propagate(PropagateRequest request) {
        return client.propagate(request).await().atMost(DEADLINE);
    }

    public VisibilityFootprint getVisibilityFootprint(GetVisibilityFootprintRequest request) {
        return client.getVisibilityFootprint(request).await().atMost(DEADLINE);
    }
}
