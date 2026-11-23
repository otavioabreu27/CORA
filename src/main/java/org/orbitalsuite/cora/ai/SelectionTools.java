package org.orbitalsuite.cora.ai;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Tools the LLM can use to request changes to the UI selection. */
@ApplicationScoped
public class SelectionTools {

    @Inject
    ActionCollector actions;

    @Tool("Selects satellites of interest by NORAD/catId. Use when the user asks to show or add satellites.")
    public String selectSatellites(@P("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("ids must contain positive NORAD/catId values");
        }
        actions.add("selectSatellites", ids.stream().map(String::valueOf).toList());
        return "Action registered: select satellites " + ids;
    }

    @Tool("Selects and follows a satellite on the globe, keeping the camera tracking its orbital position. Use when the user asks to see, show, focus, follow, or track a specific satellite. Query the catalog to get its NORAD/catId.")
    public String followSatellite(@P("id") long id) {
        if (id <= 0) throw new IllegalArgumentException("NORAD/catId must be positive");
        actions.add("followSatellite", List.of(String.valueOf(id)));
        return "Action registered: select and follow satellite " + id + " no globo";
    }

    @Tool("Selects stations of interest by catalog code, for example INPE-ALC or ESA-MLG.")
    public String selectStations(@P("ids") List<String> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("ids must contain station codes");
        }
        actions.add("selectStations", ids);
        return "Action registered: select stations " + ids;
    }

    @Tool("Focuses a station on the globe by catalog code.")
    public String focusStation(@P("id") String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("station code is required");
        actions.add("focusStation", List.of(id));
        return "Action registered: focus station " + id;
    }

    @Tool("Clears the satellite and station selection of interest from the visualization.")
    public String clearSelection() {
        actions.add("clearSelection", List.of());
        return "Action registered: clear selection";
    }
}
