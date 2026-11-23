package org.orbitalsuite.cora.ai;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
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
import org.orbitalsuite.orbital_dynamics.grpc.v1.SatelliteFilter;
import org.orbitalsuite.orbital_dynamics.grpc.v1.StationFilter;
import org.orbitalsuite.orbital_dynamics.grpc.v1.StationSource;
import org.orbitalsuite.orbital_dynamics.grpc.v1.StationStatus;
import org.orbitalsuite.orbital_dynamics.grpc.v1.TlePolicy;
import org.orbitalsuite.orbital_dynamics.grpc.v1.TlePolicyOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrbitalTools {

    @Inject
    OrbitalDynamicsClient orbital;

    @Tool("Lists satellites; name is an optional substring, catId is the optional NORAD.")
    public String listSatellites(
            @P(value = "Trecho do nome, por exemplo AMAZONIA. Omita se buscar por NORAD.", required = false) String name,
            @P(value = "NORAD/catId positivo conhecido. Omita quando buscar pelo nome.", required = false) Long catId,
            @P(value = "Known catalog group code. Omit it if not requested; do not invent a group.", required = false) Integer group) {
        var filter = SatelliteFilter.newBuilder().setCollection(Collection.SATELLITES);
        if (name != null && !name.isBlank()) {
            filter.setName(name).setNameMatch(NameMatch.NAME_CONTAINS);
        }
        if (catId != null && catId > 0) {
            filter.addCatIds(catId);
        }
        if (group != null && group > 0) {
            filter.setGroup(group);
        }
        return json(orbital.listSatellites(ListSatellitesRequest.newBuilder()
                .setFilter(filter).setPageSize(20).build()));
    }

    @Tool("Lists stations; source=INPE or ESA, status=ACTIVE/INACTIVE/PLANNED.")
    public String listStations(@P(value = "source: INPE or ESA; omit for all", required = false) String source,
                               @P(value = "status: ACTIVE, INACTIVE, or PLANNED; omit for all", required = false) String status,
                               @P(value = "query: name or code substring; omit if not needed", required = false) String query) {
        var filter = StationFilter.newBuilder();
        if (source != null && !source.isBlank()) {
            filter.setSource(OrbitalInputs.enumValue(StationSource.class, "STATION_SOURCE_" + source));
        }
        if (status != null && !status.isBlank()) {
            filter.addStatus(OrbitalInputs.enumValue(StationStatus.class, "STATION_STATUS_" + status));
        }
        if (query != null && !query.isBlank()) {
            filter.setQuery(query);
        }
        return json(orbital.listStations(ListStationsRequest.newBuilder()
                .setFilter(filter).setPageSize(20).build()));
    }

    @Tool("Predicts passes. Use tlePolicy=TLE_POLICY_AS_OF for historical replay.")
    public String predictPasses(@P("catId") long catId,
                                @P("stationCode") String stationCode,
                                @P("start") String start,
                                @P("end") String end,
                                @P(value = "tickSeconds", required = false) Integer tickSeconds,
                                @P(value = "tlePolicy", required = false) String tlePolicy,
                                @P(value = "maxTleAgeHours", required = false) Double maxTleAgeHours) {
        var request = PredictPassesRequest.newBuilder()
                .setObject(ObjectRef.newBuilder()
                        .setCollection(Collection.SATELLITES)
                        .setCatId(OrbitalInputs.positiveId(catId, "catId")))
                .setStationCode(OrbitalInputs.requiredText(stationCode, "stationCode"))
                .setWindow(OrbitalInputs.window(start, end))
                .setTickSeconds(OrbitalInputs.defaultableRange(
                        tickSeconds == null ? 60 : tickSeconds, "tickSeconds", 1, 3600))
                .setTle(tleOptions(tlePolicy, maxTleAgeHours));
        return json(orbital.predictPasses(request.build()));
    }

    @Tool("Propagates the orbit over a window. method=SGP4; tlePolicy=TLE_POLICY_AS_OF/NEAREST.")
    public String propagate(@P("catId") long catId,
                            @P("start") String start,
                            @P("end") String end,
                            @P(value = "stepSeconds", required = false) Integer stepSeconds,
                            @P(value = "tlePolicy", required = false) String tlePolicy,
                            @P(value = "maxTleAgeHours", required = false) Double maxTleAgeHours) {
        var request = PropagateRequest.newBuilder()
                .setObject(ObjectRef.newBuilder()
                        .setCollection(Collection.SATELLITES)
                        .setCatId(OrbitalInputs.positiveId(catId, "catId")))
                .setMethod(PropagationMethod.METHOD_SGP4)
                .setWindow(OrbitalInputs.window(start, end))
                .setStepSeconds(OrbitalInputs.defaultableRange(
                        stepSeconds == null ? 60 : stepSeconds, "stepSeconds", 1, 3600))
                .setTle(tleOptions(tlePolicy, maxTleAgeHours));
        return json(orbital.propagate(request.build()));
    }

    @Tool("Calculates the WGS84 footprint; provide a fixed altitude or catId+at.")
    public String getVisibilityFootprint(@P("stationCode") String stationCode,
                                         @P(value = "satelliteAltitudeM: use a fixed altitude or catId and at", required = false) Double altitudeM,
                                         @P(value = "catId: required when a fixed altitude is not provided", required = false) Long catId,
                                         @P(value = "at: ISO 8601 instant, required with catId", required = false) String at,
                                         @P(value = "refraction", required = false) Boolean refraction,
                                         @P(value = "samples", required = false) Integer samples) {
        var request = GetVisibilityFootprintRequest.newBuilder()
                .setStationCode(OrbitalInputs.requiredText(stationCode, "stationCode"))
                .setRefraction(Boolean.TRUE.equals(refraction))
                .setSamples(OrbitalInputs.defaultableRange(
                        samples == null ? 36 : samples, "samples", 8, 720));
        if (altitudeM != null) {
            request.setSatelliteAltitudeM(OrbitalInputs.positive(altitudeM, "satelliteAltitudeM"));
        } else {
            request.setObject(ObjectRef.newBuilder()
                    .setCollection(Collection.SATELLITES)
                    .setCatId(OrbitalInputs.positiveId(catId, "catId")));
            request.setAt(OrbitalInputs.timestamp(at));
        }
        return json(orbital.getVisibilityFootprint(request.build()));
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

    private String json(com.google.protobuf.MessageOrBuilder message) {
        try {
            return JsonFormat.printer().print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("invalid gRPC response", e);
        }
    }
}
