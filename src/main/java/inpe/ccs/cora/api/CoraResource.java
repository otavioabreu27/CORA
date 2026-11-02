package inpe.ccs.cora.api;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;

import inpe.ccs.cora.client.OrbitalDynamicsClient;
import inpe.ccs.orbital_dynamics.grpc.v1.Collection;
import inpe.ccs.orbital_dynamics.grpc.v1.GetVisibilityFootprintRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.ListSatellitesRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.ListStationsRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.NameMatch;
import inpe.ccs.orbital_dynamics.grpc.v1.ObjectRef;
import inpe.ccs.orbital_dynamics.grpc.v1.PredictPassesRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.PropagateRequest;
import inpe.ccs.orbital_dynamics.grpc.v1.PropagationMethod;
import inpe.ccs.orbital_dynamics.grpc.v1.TlePolicy;
import inpe.ccs.orbital_dynamics.grpc.v1.TlePolicyOptions;
import inpe.ccs.orbital_dynamics.grpc.v1.SatelliteFilter;
import inpe.ccs.orbital_dynamics.grpc.v1.StationFilter;
import inpe.ccs.orbital_dynamics.grpc.v1.StationSource;
import inpe.ccs.orbital_dynamics.grpc.v1.StationStatus;
import inpe.ccs.orbital_dynamics.grpc.v1.TimeWindow;
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
        return json(orbital.getElements(catId));
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
            filter.addCatIds(catId);
        }
        if (group != null && group > 0) {
            filter.setGroup(group);
        }
        var request = ListSatellitesRequest.newBuilder()
                .setFilter(filter)
                .setPageSize(pageSize == null ? 0 : pageSize)
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
            filter.setSource(enumValue(StationSource.class, "STATION_SOURCE_" + source));
        }
        if (status != null && !status.isBlank()) {
            filter.addStatus(enumValue(StationStatus.class, "STATION_STATUS_" + status));
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
                .setPageSize(pageSize == null ? 0 : pageSize)
                .setPageToken(pageToken == null ? "" : pageToken)
                .build();
        return json(orbital.listStations(request));
    }

    @POST
    @Path("/passes")
    public String passes(PassRequest body) {
        if (body == null || body.catId() == null || body.stationCode() == null
                || body.start() == null || body.end() == null) {
            throw new BadRequestException("catId, stationCode, start e end são obrigatórios");
        }
        var request = PredictPassesRequest.newBuilder()
                .setObject(ObjectRef.newBuilder()
                        .setCollection(Collection.SATELLITES)
                        .setCatId(body.catId()))
                .setStationCode(body.stationCode())
                .setWindow(TimeWindow.newBuilder()
                        .setStart(timestamp(body.start()))
                        .setEnd(timestamp(body.end())))
                .setStepSeconds(body.stepSeconds() == null ? 0 : body.stepSeconds())
                .setTickSeconds(body.tickSeconds() == null ? 0 : body.tickSeconds())
                .setIncludeFootprint(Boolean.TRUE.equals(body.includeFootprint()))
                .setIncludeTickFootprints(Boolean.TRUE.equals(body.includeTickFootprints()))
                .setRefraction(Boolean.TRUE.equals(body.refraction()))
                .setFootprintSamples(body.footprintSamples() == null ? 0 : body.footprintSamples());
        if (body.downlinkFrequencyHz() != null) {
            request.setDownlinkFrequencyHz(body.downlinkFrequencyHz());
        }
        request.setTle(tleOptions(body.tlePolicy(), body.maxTleAgeHours()));
        return json(orbital.predictPasses(request.build()));
    }

    @POST
    @Path("/orbit")
    public String orbit(OrbitRequest body) {
        if (body == null || body.catId() == null || body.start() == null || body.end() == null) {
            throw new BadRequestException("catId, start e end são obrigatórios");
        }
        var method = body.method() == null || body.method().isBlank()
                         ? PropagationMethod.METHOD_SGP4
                         : enumValue(PropagationMethod.class,
                                     body.method().toUpperCase().startsWith("METHOD_")
                                         ? body.method()
                                         : "METHOD_" + body.method());
        var request = PropagateRequest.newBuilder()
                .setObject(ObjectRef.newBuilder()
                        .setCollection(Collection.SATELLITES)
                        .setCatId(body.catId()))
                .setMethod(method)
                .setWindow(TimeWindow.newBuilder()
                        .setStart(timestamp(body.start()))
                        .setEnd(timestamp(body.end())))
                .setStepSeconds(body.stepSeconds() == null ? 0 : body.stepSeconds())
                .setTle(tleOptions(body.tlePolicy(), body.maxTleAgeHours()));
        return json(orbital.propagate(request.build()));
    }

    @POST
    @Path("/footprint")
    public String footprint(FootprintRequest body) {
        if (body == null || body.stationCode() == null
                || (body.satelliteAltitudeM() == null && (body.catId() == null || body.at() == null))) {
            throw new BadRequestException(
                    "stationCode e satelliteAltitudeM (ou catId + at) são obrigatórios");
        }
        var request = GetVisibilityFootprintRequest.newBuilder()
                .setStationCode(body.stationCode())
                .setRefraction(Boolean.TRUE.equals(body.refraction()))
                .setSamples(body.samples() == null ? 0 : body.samples());
        if (body.satelliteAltitudeM() != null) {
            request.setSatelliteAltitudeM(body.satelliteAltitudeM());
        } else {
            request.setObject(ObjectRef.newBuilder()
                    .setCollection(Collection.SATELLITES)
                    .setCatId(body.catId()));
            request.setAt(timestamp(body.at()));
        }
        if (body.minElevationDeg() != null) {
            request.setMinElevationDeg(body.minElevationDeg());
        }
        return json(orbital.getVisibilityFootprint(request.build()));
    }

    private String json(com.google.protobuf.MessageOrBuilder message) {
        try {
            return JsonFormat.printer().print(message);
        } catch (InvalidProtocolBufferException e) {
            Log.error("Falha ao serializar resposta protobuf", e);
            throw new IllegalStateException("resposta gRPC inválida", e);
        }
    }

    private TlePolicyOptions tleOptions(String policy, Double maxAgeHours) {
        var builder = TlePolicyOptions.newBuilder();
        if (policy != null && !policy.isBlank()) {
            String name = policy.toUpperCase().replace('-', '_');
            if (!name.startsWith("TLE_POLICY_")) {
                name = "TLE_POLICY_" + name;
            }
            builder.setPolicy(enumValue(TlePolicy.class, name));
        }
        if (maxAgeHours != null) {
            builder.setMaxAgeHours(maxAgeHours);
        }
        return builder.build();
    }

    private Timestamp timestamp(String value) {
        try {
            var instant = Instant.parse(value);
            return Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
        } catch (DateTimeParseException e) {
            throw new BadRequestException("timestamp inválido (use RFC 3339 UTC): " + value);
        }
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("valor inválido: " + value);
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
