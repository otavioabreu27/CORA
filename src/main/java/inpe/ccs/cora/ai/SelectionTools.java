package inpe.ccs.cora.ai;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Ferramentas que o LLM pode usar para pedir mudanças na seleção da UI. */
@ApplicationScoped
public class SelectionTools {

    @Inject
    ActionCollector actions;

    @Tool("Seleciona satélites de interesse pelo NORAD/catId. Use quando o usuário pedir para mostrar/adicionar satélites.")
    public String selectSatellites(@P("ids") List<Long> ids) {
        actions.add("selectSatellites", ids.stream().map(String::valueOf).toList());
        return "Ação registrada: selecionar satélites " + ids;
    }

    @Tool("Seleciona e segue um satélite no globo, mantendo a câmera acompanhando sua posição orbital. Use quando o usuário pedir para ver, mostrar, focar, seguir ou acompanhar um satélite específico. Consulte o catálogo para obter seu NORAD/catId.")
    public String followSatellite(@P("id") long id) {
        if (id <= 0) throw new IllegalArgumentException("NORAD/catId deve ser positivo");
        actions.add("followSatellite", List.of(String.valueOf(id)));
        return "Ação registrada: selecionar e seguir o satélite " + id + " no globo";
    }

    @Tool("Seleciona estações de interesse pelo código do catálogo, por exemplo INPE-ALC ou ESA-MLG.")
    public String selectStations(@P("ids") List<String> ids) {
        actions.add("selectStations", ids);
        return "Ação registrada: selecionar estações " + ids;
    }

    @Tool("Coloca uma estação em foco no globo, pelo código do catálogo.")
    public String focusStation(@P("id") String id) {
        actions.add("focusStation", List.of(id));
        return "Ação registrada: focar estação " + id;
    }

    @Tool("Limpa a seleção de satélites e estações de interesse da visualização.")
    public String clearSelection() {
        actions.add("clearSelection", List.of());
        return "Ação registrada: limpar seleção";
    }
}
