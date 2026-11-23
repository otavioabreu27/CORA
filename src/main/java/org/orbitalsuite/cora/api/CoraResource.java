package org.orbitalsuite.cora.api;

import java.util.function.Supplier;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import org.orbitalsuite.cora.OrbitalInputs;
import org.orbitalsuite.cora.client.OrbitalDynamicsClient;
import org.orbitalsuite.orbital_dynamics.grpc.v1.Collection;
import org.orbitalsuite.orbital_dynamics.grpc.v1.GetVisibilityFootprintRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.ListSatellitesRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.ListStationsRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.NameMatch;
import org.orbitalsuite.orbital_dynamics.grpc.v1.ObjectRef;
import org.orbitalsuite.orbital_dynamics.grpc.v1.PredictPassesRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.PropagateRequest;
import org.orbitalsuite.orbital_dynamics.grpc.v1.PropagationMethod;
import org.orbitalsuite.orbital_dynamics.grpc.v1.TlePolicy;
import org.orbitalsuite.orbital_dynamics.grpc.v1.TlePolicyOptions;
import org.orbitalsuite.orbital_dynamics.grpc.v1.SatelliteFilter;
import org.orbitalsuite.orbital_dynamics.grpc.v1.StationFilter;
import org.orbitalsuite.orbital_dynamics.grpc.v1.StationSource;
import org.orbitalsuite.orbital_dynamics.grpc.v1.StationStatus;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class CoraResource {

    @Inject
    OrbitalDynamicsClient orbital;

    @ConfigProperty(name = "quarkus.application.name")
    String application;

    @ConfigProperty(name = "service.level")
    String level;

    @GET
    @Path("/info")
    public Info info() {
        return new Info(application, level);
    }

    @GET
    @Path("/elements/{catId}")
    public String elements(@PathParam("catId") long catId) {
        return json(orbital.getElements(input(() -> OrbitalInputs.positiveId(catId, "catId"))));
    }

    @GET
    @Path("/satellites")
    public String satellites(@QueryParam("name") String name,
                             @QueryParam("catId") Long catId,
                             @QueryParam("group") Integer group,
                             @QueryParam("pageSize") Integer pageSize,
                             @QueryParam("pageToken") String pageToken) {
        var filter = SatelliteFilter.newBuilder().setCollection(Collection.SATELLITES);
        if (name != null && !name.isBlank()) {
            filter.setName(name).setNameMatch(NameMatch.NAME_CONTAINS);
        }
        if (catId != null) {
            filter.addCatIds(input(() -> OrbitalInputs.positiveId(catId, "catId")));
        }
        if (group != null) {
            filter.setGroup(input(() -> Math.toIntExact(OrbitalInputs.positiveId(group.longValue(), "group"))));
        }
        var request = ListSatellitesRequest.newBuilder()
                .setFilter(filter)
                .setPageSize(input(() -> OrbitalInputs.defaultableRange(pageSize, "pageSize", 1, 500)))
                .setPageToken(pageToken == null ? "" : pageToken)
                .build();
        return json(orbital.listSatellites(request));
    }

    @GET
    @Path("/stations")
    public String stations(@QueryParam("source") String source,
                           @QueryParam("status") String status,
                           @QueryParam("countryCode") String countryCode,
                           @QueryParam("operator") String operator,
                           @QueryParam("query") String query,
                           @QueryParam("pageSize") Integer pageSize,
                           @QueryParam("pageToken") String pageToken) {
        var filter = StationFilter.newBuilder();
        if (source != null && !source.isBlank()) {
            filter.setSource(input(() -> OrbitalInputs.enumValue(
                    StationSource.class, "STATION_SOURCE_" + source)));
        }
        if (status != null && !status.isBlank()) {
            filter.addStatus(input(() -> OrbitalInputs.enumValue(
                    StationStatus.class, "STATION_STATUS_" + status)));
        }
        if (countryCode != null && !countryCode.isBlank()) {
            filter.setCountryCode(countryCode);
        }
        if (operator != null && !operator.isBlank()) {
            filter.setOperator(operator);
        }
        if (query != null && !query.isBlank()) {
            filter.setQuery(query);
        }
        var request = ListStationsRequest.newBuilder()
                .setFilter(filter)
                .setPageSize(input(() -> OrbitalInputs.defaultableRange(pageSize, "pageSize", 1, 500)))
                .setPageToken(pageToken == null ? "" : pageToken)
                .build();
        return json(orbital.listStations(request));
    }

    @POST
    @Path("/passes")
    public String passes(PassRequest body) {
        var request = input(() -> passRequest(body));
        return json(orbital.predictPasses(request));
    }

    private PredictPassesRequest passRequest(PassRequest body) {
        if (body == null) {
            throw new IllegalArgumentException("request body is required");
        }
        var request = PredictPassesRequest.newBuilder()
                .setObject(ObjectRef.newBuilder()
                        .setCollection(Collection.SATELLITES)
                        .setCatId(OrbitalInputs.positiveId(body.catId(), "catId")))
                .setStationCode(OrbitalInputs.requiredText(body.stationCode(), "stationCode"))
                .setWindow(OrbitalInputs.window(body.start(), body.end()))
                .setStepSeconds(OrbitalInputs.defaultableRange(body.stepSeconds(), "stepSeconds", 1, 3600))
                .setIncludeFootprint(Boolean.TRUE.equals(body.includeFootprint()))
                .setIncludeTickFootprints(Boolean.TRUE.equals(body.includeTickFootprints()))
                .setRefraction(Boolean.TRUE.equals(body.refraction()))
                .setFootprintSamples(OrbitalInputs.defaultableRange(body.footprintSamples(), "footprintSamples", 8, 720));
        if (body.tickSeconds() != null) {
            request.setTickSeconds(OrbitalInputs.defaultableRange(body.tickSeconds(), "tickSeconds", 1, 3600));
        }
        if (body.downlinkFrequencyHz() != null) {
            request.setDownlinkFrequencyHz(OrbitalInputs.positive(body.downlinkFrequencyHz(), "downlinkFrequencyHz"));
        }
        request.setTle(tleOptions(body.tlePolicy(), body.maxTleAgeHours()));
        return request.build();
    }

    @POST
    @Path("/orbit")
    public String orbit(OrbitRequest body) {
        var request = input(() -> orbitRequest(body));
        return json(orbital.propagate(request));
    }

    private PropagateRequest orbitRequest(OrbitRequest body) {
        if (body == null) {
            throw new IllegalArgumentException("request body is required");
        }
        var method = body.method() == null || body.method().isBlank()
                         ? PropagationMethod.METHOD_SGP4
                         : OrbitalInputs.prefixedEnumValue(
                                 PropagationMethod.class, body.method(), "METHOD_");
        return PropagateRequest.newBuilder()
                .setObject(ObjectRef.newBuilder()
                        .setCollection(Collection.SATELLITES)
                        .setCatId(OrbitalInputs.positiveId(body.catId(), "catId")))
                .setMethod(method)
                .setWindow(OrbitalInputs.window(body.start(), body.end()))
                .setStepSeconds(OrbitalInputs.defaultableRange(body.stepSeconds(), "stepSeconds", 1, 3600))
                .setTle(tleOptions(body.tlePolicy(), body.maxTleAgeHours()))
                .build();
    }

    @POST
    @Path("/footprint")
    public String footprint(FootprintRequest body) {
        var request = input(() -> footprintRequest(body));
        return json(orbital.getVisibilityFootprint(request));
    }

    private GetVisibilityFootprintRequest footprintRequest(FootprintRequest body) {
        if (body == null) {
            throw new IllegalArgumentException("request body is required");
        }
        var request = GetVisibilityFootprintRequest.newBuilder()
                .setStationCode(OrbitalInputs.requiredText(body.stationCode(), "stationCode"))
                .setRefraction(Boolean.TRUE.equals(body.refraction()))
                .setSamples(OrbitalInputs.defaultableRange(body.samples(), "samples", 8, 720));
        if (body.satelliteAltitudeM() != null) {
            request.setSatelliteAltitudeM(OrbitalInputs.positive(body.satelliteAltitudeM(), "satelliteAltitudeM"));
        } else {
            request.setObject(ObjectRef.newBuilder()
                    .setCollection(Collection.SATELLITES)
                    .setCatId(OrbitalInputs.positiveId(body.catId(), "catId")));
            request.setAt(OrbitalInputs.timestamp(body.at()));
        }
        if (body.minElevationDeg() != null) {
            double elevation = OrbitalInputs.nonNegative(body.minElevationDeg(), "minElevationDeg");
            if (elevation > 90) {
                throw new IllegalArgumentException("minElevationDeg must not exceed 90");
            }
            request.setMinElevationDeg(elevation);
        }
        return request.build();
    }

    private String json(com.google.protobuf.MessageOrBuilder message) {
        try {
            return JsonFormat.printer().print(message);
        } catch (InvalidProtocolBufferException e) {
            Log.error("Falha ao serializar resposta protobuf", e);
            throw new IllegalStateException("invalid gRPC response", e);
        }
    }

    private TlePolicyOptions tleOptions(String policy, Double maxAgeHours) {
        var builder = TlePolicyOptions.newBuilder();
        if (policy != null && !policy.isBlank()) {
            builder.setPolicy(OrbitalInputs.prefixedEnumValue(
                    TlePolicy.class, policy, "TLE_POLICY_"));
        }
        if (maxAgeHours != null) {
            builder.setMaxAgeHours(OrbitalInputs.nonNegative(maxAgeHours, "maxTleAgeHours"));
        }
        return builder.build();
    }

    private <T> T input(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    public record Info(String application, String level) {}

    public record PassRequest(Long catId, String stationCode, String start, String end,
                              Integer stepSeconds, Integer tickSeconds,
                              Double downlinkFrequencyHz, Boolean includeFootprint,
                              Boolean includeTickFootprints, Boolean refraction,
                              Integer footprintSamples, String tlePolicy,
                              Double maxTleAgeHours) {}

    public record OrbitRequest(Long catId, String start, String end, Integer stepSeconds,
                               String method, String tlePolicy, Double maxTleAgeHours) {}

    public record FootprintRequest(String stationCode, Double satelliteAltitudeM, Long catId,
                                   String at, Double minElevationDeg, Boolean refraction,
                                   Integer samples) {}
}
