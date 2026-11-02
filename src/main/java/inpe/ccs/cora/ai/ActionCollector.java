package inpe.ccs.cora.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

/** Coleta ações de UI emitidas pelas ferramentas durante uma chamada de chat. */
@ApplicationScoped
public class ActionCollector {

    public record Action(String type, List<String> ids, Map<String, Object> parameters) {
        public Action(String type, List<String> ids) { this(type, ids, Map.of()); }
    }

    private final ThreadLocal<List<Action>> actions = ThreadLocal.withInitial(ArrayList::new);

    public void clear() {
        actions.get().clear();
    }

    public void add(String type, List<String> ids) {
        actions.get().add(new Action(type, List.copyOf(ids)));
    }

    public void add(String type, Map<String, Object> parameters) {
        actions.get().add(new Action(type, List.of(), Map.copyOf(parameters)));
    }

    public List<Action> snapshot() {
        return List.copyOf(actions.get());
    }
}
