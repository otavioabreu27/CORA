package inpe.ccs.cora.ai;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Requests UI changes only; orbital calculations remain in the L2. */
@ApplicationScoped
public class VisualizationTools {
    @Inject ActionCollector actions;

    @Tool("Stops satellite camera following while preserving the selection.")
    public String stopFollowing() {
        actions.add("stopFollowing", List.of());
        return "Action registered: stop following the satellite";
    }

    @Tool("Moves the globe camera. view=world for the world view, brazil for Brazil, or home for the initial view. Ends focus and following.")
    public String setCamera(@P("view") String view) {
        if (!List.of("world", "brazil", "home").contains(view)) throw new IllegalArgumentException("view must be world, brazil, or home");
        actions.add("setCamera", Map.of("view", view));
        return "Action registered: camera " + view;
    }

    @Tool("Shows or hides globe layers. orbits controls trails; coverage controls the visibility area of the focused station. Null preserves the layer.")
    public String setLayers(@P(value = "orbits", required = false) Boolean orbits,
                            @P(value = "coverage", required = false) Boolean coverage) {
        var parameters = new HashMap<String, Object>();
        if (orbits != null) parameters.put("orbits", orbits);
        if (coverage != null) parameters.put("coverage", coverage);
        if (parameters.isEmpty()) throw new IllegalArgumentException("Provide orbits or coverage");
        actions.add("setLayers", parameters);
        return "Action registered: update layers " + parameters;
    }

    @Tool("Changes the displayed time and pauses orbital playback. at must be ISO 8601 with a time zone, for example 2026-11-02T18:00:00Z. It does not change the orbital element epoch.")
    public String setTime(@P("at") String at) {
        var instant = Instant.parse(at);
        actions.add("setTime", Map.of("at", instant.toString()));
        return "Action registered: displayed time " + instant;
    }

    @Tool("Controls orbital playback speed. rate=0 pauses, 1 is normal speed, 30 is 30x, 120 is 120x.")
    public String setPlayback(@P("rate") int rate) {
        if (!List.of(0, 1, 30, 120).contains(rate)) throw new IllegalArgumentException("rate must be 0, 1, 30, or 120");
        actions.add("setPlayback", Map.of("rate", rate));
        return "Action registered: speed " + rate;
    }

    @Tool("Returns to the current time and normal orbital visualization speed.")
    public String goLive() {
        actions.add("goLive", List.of());
        return "Action registered: return to current time";
    }

    @Tool("Opens a UI panel. panel=stations (station catalog), satellites (satellites and time), visualization (layers and camera), passes (passes for the focused station; use focusStation first).")
    public String openPanel(@P("panel") String panel) {
        if (!List.of("stations", "satellites", "visualization", "passes").contains(panel)) throw new IllegalArgumentException("Unsupported panel");
        actions.add("openPanel", Map.of("panel", panel));
        return "Action registered: open panel " + panel;
    }
}
