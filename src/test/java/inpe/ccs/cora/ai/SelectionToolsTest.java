package inpe.ccs.cora.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SelectionToolsTest {
    @Test void followSatelliteEmitsTheUiContractWithoutChangingOrbitalData() {
        var collector = new ActionCollector();
        var tools = new SelectionTools();
        tools.actions = collector;
        collector.clear();
        tools.followSatellite(47699);
        assertEquals(List.of(new ActionCollector.Action("followSatellite", List.of("47699"))), collector.snapshot());
        tools.clearSelection();
        assertEquals("clearSelection", collector.snapshot().getLast().type());
    }

    @Test void rejectsInvalidSatelliteIdsWithoutEmittingAnAction() {
        var collector = new ActionCollector();
        var tools = new SelectionTools();
        tools.actions = collector;
        collector.clear();
        assertThrows(IllegalArgumentException.class, () -> tools.followSatellite(0));
        assertThrows(IllegalArgumentException.class, () -> tools.followSatellite(-1));
        assertEquals(List.of(), collector.snapshot());
    }
}
