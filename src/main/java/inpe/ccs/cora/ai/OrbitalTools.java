package inpe.ccs.cora.ai;

import java.time.Instant;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
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
import inpe.ccs.orbital_dynamics.grpc.v1.SatelliteFilter;
import inpe.ccs.orbital_dynamics.grpc.v1.StationFilter;
import inpe.ccs.orbital_dynamics.grpc.v1.StationSource;
import inpe.ccs.orbital_dynamics.grpc.v1.StationStatus;
import inpe.ccs.orbital_dynamics.grpc.v1.TlePolicy;
import inpe.ccs.orbital_dynamics.grpc.v1.TlePolicyOptions;
import inpe.ccs.orbital_dynamics.grpc.v1.TimeWindow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrbitalTools {

    @Inject
    OrbitalDynamicsClient orbital;

    @Tool("Lista satélites; name é um trecho opcional, catId é o NORAD opcional.")
    public String listSatellites(
            @P(value = "Trecho do nome, por exemplo AMAZONIA. Omita se buscar por NORAD.", required = false) String name,
            @P(value = "NORAD/catId positivo conhecido. Omita quando buscar pelo nome.", required = false) Long catId,
            @P(value = "Código de grupo conhecido do catálogo. Omita se não foi solicitado; não invente um grupo.", required = false) Integer group) {
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

    @Tool("Lista estações; source=INPE ou ESA, status=ACTIVE/INACTIVE/PLANNED.")
    public String listStations(@P(value = "source: INPE ou ESA; omita para todas", required = false) String source,
                               @P(value = "status: ACTIVE, INACTIVE ou PLANNED; omita para todos", required = false) String status,
                               @P(value = "query: trecho do nome ou código; omita se não necessário", required = false) String query) {
        var filter = StationFilter.newBuilder();
        if (source != null && !source.isBlank()) {
            filter.setSource(enumValue(StationSource.class, "STATION_SOURCE_" + source));
        }
        if (status != null && !status.isBlank()) {
            filter.addStatus(enumValue(StationStatus.class, "STATION_STATUS_" + status));
        }
        if (query != null && !query.isBlank()) {
            filter.setQuery(query);
        }
        return json(orbital.listStations(ListStationsRequest.newBuilder()
                .setFilter(filter).setPageSize(20).build()));
    }

    @Tool("Prevê passagens. Use tlePolicy=TLE_POLICY_AS_OF para replay histórico.")
    public String predictPasses(@P("catId") long catId,
                                @P("stationCode") String stationCode,
                                @P("start") String start,
                                @P("end") String end,
                                @P(value = "tickSeconds", required = false) Integer tickSeconds,
                                @P(value = "tlePolicy", required = false) String tlePolicy,
                                @P(value = "maxTleAgeHours", required = false) Double maxTleAgeHours) {
        var request = PredictPassesRequest.newBuilder()
                .setObject(ObjectRef.newBuilder()
                        .setCollection(Collection.SATELLITES).setCatId(catId))
                .setStationCode(stationCode)
                .setWindow(TimeWindow.newBuilder().setStart(timestamp(start)).setEnd(timestamp(end)))
                .setTickSeconds(tickSeconds == null ? 60 : tickSeconds)
                .setTle(tleOptions(tlePolicy, maxTleAgeHours));
        return json(orbital.predictPasses(request.build()));
    }

    @Tool("Propaga a órbita em uma janela. method=SGP4; tlePolicy=TLE_POLICY_AS_OF/NEAREST.")
    public String propagate(@P("catId") long catId,
                            @P("start") String start,
                            @P("end") String end,
                            @P(value = "stepSeconds", required = false) Integer stepSeconds,
                            @P(value = "tlePolicy", required = false) String tlePolicy,
                            @P(value = "maxTleAgeHours", required = false) Double maxTleAgeHours) {
        var request = PropagateRequest.newBuilder()
                .setObject(ObjectRef.newBuilder()
                        .setCollection(Collection.SATELLITES).setCatId(catId))
                .setMethod(PropagationMethod.METHOD_SGP4)
                .setWindow(TimeWindow.newBuilder().setStart(timestamp(start)).setEnd(timestamp(end)))
                .setStepSeconds(stepSeconds == null ? 60 : stepSeconds)
                .setTle(tleOptions(tlePolicy, maxTleAgeHours));
        return json(orbital.propagate(request.build()));
    }

    @Tool("Calcula o footprint WGS84; informe altitude fixa ou catId+at.")
    public String getVisibilityFootprint(@P("stationCode") String stationCode,
                                         @P(value = "satelliteAltitudeM: use altitude fixa ou catId e at", required = false) Double altitudeM,
                                         @P(value = "catId: necessário quando altitude fixa não é informada", required = false) Long catId,
                                         @P(value = "at: instante ISO 8601, necessário junto com catId", required = false) String at,
                                         @P(value = "refraction", required = false) Boolean refraction,
                                         @P(value = "samples", required = false) Integer samples) {
        var request = GetVisibilityFootprintRequest.newBuilder()
                .setStationCode(stationCode)
                .setRefraction(Boolean.TRUE.equals(refraction))
                .setSamples(samples == null ? 36 : samples);
        if (altitudeM != null) {
            request.setSatelliteAltitudeM(altitudeM);
        } else {
            request.setObject(ObjectRef.newBuilder()
                    .setCollection(Collection.SATELLITES).setCatId(catId));
            request.setAt(timestamp(at));
        }
        return json(orbital.getVisibilityFootprint(request.build()));
    }

    private TlePolicyOptions tleOptions(String policy, Double maxAgeHours) {
        var builder = TlePolicyOptions.newBuilder();
        if (policy != null && !policy.isBlank()) {
            var name = policy.toUpperCase().replace('-', '_');
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
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("timestamp é obrigatório");
        }
        var instant = Instant.parse(value);
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano()).build();
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return Enum.valueOf(type, value.toUpperCase().replace('-', '_'));
    }

    private String json(com.google.protobuf.MessageOrBuilder message) {
        try {
            return JsonFormat.printer().print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("resposta gRPC inválida", e);
        }
    }
}
