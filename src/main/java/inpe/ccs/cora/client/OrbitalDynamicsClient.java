package inpe.ccs.cora.client;

import java.time.Duration;

import inpe.ccs.orbital_dynamics.grpc.v1.Elements;
import inpe.ccs.orbital_dynamics.grpc.v1.ElementsPage;
import inpe.ccs.orbital_dynamics.grpc.v1.GetVisibilityFootprintRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.ListSatellitesRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.ListStationsRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.OrbitalDynamics;
import inpe.ccs.orbital_dynamics.grpc.v1.PredictPassesRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.PredictPassesResponse;
import inpe.ccs.orbital_dynamics.grpc.v1.PropagateRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.PropagateResponse;
import inpe.ccs.orbital_dynamics.grpc.v1.StationPage;
import inpe.ccs.orbital_dynamics.grpc.v1.VisibilityFootprint;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;

/** Fachada bloqueante do client gRPC do L2 orbital-dynamics. */
@ApplicationScoped
public class OrbitalDynamicsClient {

    private static final Duration DEADLINE = Duration.ofSeconds(15);

    @GrpcClient("orbital-dynamics")
    OrbitalDynamics client;

    public Elements getElements(long catId) {
        var request = inpe.ccs.orbital_dynamics.grpc.v1.GetElementsRequest.newBuilder()
                .setObject(inpe.ccs.orbital_dynamics.grpc.v1.ObjectRef.newBuilder()
                        .setCollection(inpe.ccs.orbital_dynamics.grpc.v1.Collection.SATELLITES)
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
