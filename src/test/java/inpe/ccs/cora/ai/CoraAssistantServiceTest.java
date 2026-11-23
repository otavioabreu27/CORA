package inpe.ccs.cora.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class CoraAssistantServiceTest {

    @Test void returnsActionsAndReleasesThreadStateAfterSuccess() {
        var collector = new ActionCollector();
        var service = service(collector, message -> {
            collector.add("followSatellite", List.of("44883"));
            return "tracking";
        });

        var result = service.chat("show CBERS 4A");

        assertEquals("tracking", result.answer());
        assertEquals("followSatellite", result.actions().getFirst().type());
        assertEquals(List.of(), collector.snapshot());
    }

    @Test void releasesThreadStateWhenTheProviderFails() {
        var collector = new ActionCollector();
        var service = service(collector, message -> {
            collector.add("focusStation", List.of("INPE-ALC"));
            throw new IllegalStateException("provider failed");
        });

        assertThrows(IllegalStateException.class, () -> service.chat("focus Alcantara"));
        assertEquals(List.of(), collector.snapshot());
    }

    private CoraAssistantService service(ActionCollector collector, CoraAssistant assistant) {
        var service = new CoraAssistantService();
        service.enabled = true;
        service.apiKey = "test-key";
        service.actions = collector;
        try {
            var field = CoraAssistantService.class.getDeclaredField("assistant");
            field.setAccessible(true);
            field.set(service, assistant);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return service;
    }
}
