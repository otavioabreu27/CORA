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

    @Tool("Interrompe o acompanhamento de satélite na câmera, preservando a seleção.")
    public String stopFollowing() {
        actions.add("stopFollowing", List.of());
        return "Ação registrada: parar de seguir o satélite";
    }

    @Tool("Move a câmera do globo. view=world para visão mundial, brazil para Brasil ou home para visão inicial. Encerra foco e acompanhamento.")
    public String setCamera(@P("view") String view) {
        if (!List.of("world", "brazil", "home").contains(view)) throw new IllegalArgumentException("view deve ser world, brazil ou home");
        actions.add("setCamera", Map.of("view", view));
        return "Ação registrada: câmera " + view;
    }

    @Tool("Mostra ou oculta camadas do globo. orbits controla trajetórias; coverage controla área de visibilidade da estação em foco. Null preserva a camada.")
    public String setLayers(@P(value = "orbits", required = false) Boolean orbits,
                            @P(value = "coverage", required = false) Boolean coverage) {
        var parameters = new HashMap<String, Object>();
        if (orbits != null) parameters.put("orbits", orbits);
        if (coverage != null) parameters.put("coverage", coverage);
        if (parameters.isEmpty()) throw new IllegalArgumentException("Informe orbits ou coverage");
        actions.add("setLayers", parameters);
        return "Ação registrada: atualizar camadas " + parameters;
    }

    @Tool("Altera o horário visualizado e pausa a reprodução orbital. at deve ser ISO 8601 com fuso, por exemplo 2026-11-02T18:00:00Z. Não altera a época dos elementos orbitais.")
    public String setTime(@P("at") String at) {
        var instant = Instant.parse(at);
        actions.add("setTime", Map.of("at", instant.toString()));
        return "Ação registrada: horário visualizado " + instant;
    }

    @Tool("Controla a velocidade da reprodução orbital. rate=0 pausa, 1 tempo normal, 30 acelera 30 vezes, 120 acelera 120 vezes.")
    public String setPlayback(@P("rate") int rate) {
        if (!List.of(0, 1, 30, 120).contains(rate)) throw new IllegalArgumentException("rate deve ser 0, 1, 30 ou 120");
        actions.add("setPlayback", Map.of("rate", rate));
        return "Ação registrada: velocidade " + rate;
    }

    @Tool("Volta ao horário atual e à velocidade normal da visualização orbital.")
    public String goLive() {
        actions.add("goLive", List.of());
        return "Ação registrada: voltar ao horário atual";
    }

    @Tool("Abre um painel da interface. panel=stations (catálogo de estações), satellites (satélites e tempo), visualization (camadas e câmera), passes (passagens da estação em foco; use focusStation antes).")
    public String openPanel(@P("panel") String panel) {
        if (!List.of("stations", "satellites", "visualization", "passes").contains(panel)) throw new IllegalArgumentException("Painel não suportado");
        actions.add("openPanel", Map.of("panel", panel));
        return "Ação registrada: abrir painel " + panel;
    }
}
