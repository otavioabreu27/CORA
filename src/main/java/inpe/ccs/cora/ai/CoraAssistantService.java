package inpe.ccs.cora.ai;

import java.time.Duration;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ServiceUnavailableException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CoraAssistantService {

    @Inject
    OrbitalTools tools;

    @Inject
    SelectionTools selectionTools;

    @Inject
    VisualizationTools visualizationTools;

    @Inject
    ActionCollector actions;

    @ConfigProperty(name = "cora.ai.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "cora.ai.base-url", defaultValue = "https://api.deepseek.com/v1")
    String baseUrl;

    @ConfigProperty(name = "cora.ai.model", defaultValue = "deepseek-chat")
    String model;

    @ConfigProperty(name = "cora.ai.api-key", defaultValue = "CHANGE_ME")
    String apiKey;

    private volatile CoraAssistant assistant;

    public record ChatResult(String answer, java.util.List<ActionCollector.Action> actions) {}

    public ChatResult chat(String message) {
        if (!enabled) {
            throw new ServiceUnavailableException(
                    "AI is disabled; set CORA_AI_ENABLED=true and configure the provider");
        }
        if (apiKey == null || apiKey.isBlank() || "CHANGE_ME".equals(apiKey)) {
            throw new ServiceUnavailableException(
                    "CORA_AI_API_KEY is not configured for the DeepSeek provider");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        actions.clear();
        try {
            var answer = assistant().chat(message);
            return new ChatResult(answer, actions.snapshot());
        } finally {
            actions.release();
        }
    }

    private CoraAssistant assistant() {
        var current = assistant;
        if (current == null) {
            synchronized (this) {
                current = assistant;
                if (current == null) {
                    var chatModel = OpenAiChatModel.builder()
                            .baseUrl(baseUrl)
                            .apiKey(apiKey)
                            .modelName(model)
                            .temperature(0.2)
                            .timeout(Duration.ofSeconds(90))
                            .build();
                    current = AiServices.builder(CoraAssistant.class)
                            .chatModel(chatModel)
                            .tools(tools, selectionTools, visualizationTools)
                            .build();
                    assistant = current;
                }
            }
        }
        return current;
    }
}
