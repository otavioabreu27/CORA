package inpe.ccs.cora.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.format.DateTimeParseException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VisualizationToolsTest {
    @Test void emitsTypedParametersInRequestOrder() {
        var collector = new ActionCollector();
        var tools = new VisualizationTools();
        tools.actions = collector;
        collector.clear();
        tools.setTime("2026-11-02T18:00:00Z");
        tools.setPlayback(30);
        tools.setLayers(false, null);
        tools.openPanel("satellites");
        assertEquals("setTime", collector.snapshot().getFirst().type());
        assertEquals(Map.of("at", "2026-11-02T18:00:00Z"), collector.snapshot().getFirst().parameters());
        assertEquals(Map.of("rate", 30), collector.snapshot().get(1).parameters());
        assertEquals(Map.of("orbits", false), collector.snapshot().get(2).parameters());
        assertEquals(Map.of("panel", "satellites"), collector.snapshot().getLast().parameters());
    }

    @Test void rejectsUnsupportedControlsBeforeEmittingActions() {
        var collector = new ActionCollector();
        var tools = new VisualizationTools();
        tools.actions = collector;
        collector.clear();
        assertThrows(IllegalArgumentException.class, () -> tools.setPlayback(999));
        assertThrows(IllegalArgumentException.class, () -> tools.setCamera("execute"));
        assertThrows(IllegalArgumentException.class, () -> tools.openPanel("admin"));
        assertThrows(IllegalArgumentException.class, () -> tools.setLayers(null, null));
        assertThrows(DateTimeParseException.class, () -> tools.setTime("2026-11-02T18:00:00"));
        assertEquals(0, collector.snapshot().size());
    }
}
