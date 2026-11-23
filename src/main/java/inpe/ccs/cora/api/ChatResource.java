package inpe.ccs.cora.api;

import java.util.List;

import inpe.ccs.cora.ai.ActionCollector;
import inpe.ccs.cora.ai.CoraAssistantService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/chat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class ChatResource {

    @Inject
    CoraAssistantService assistant;

    @POST
    public ChatResponse chat(ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new BadRequestException("message is required");
        }
        var context = new StringBuilder();
        if (request.selection() != null) {
            context.append("Current UI selection: satellites=")
                    .append(request.selection().satellites())
                    .append(", stations=")
                    .append(request.selection().stations())
                    .append(". ");
        }
        context.append(request.message());
        var result = assistant.chat(context.toString());
        return new ChatResponse(result.answer(), result.actions());
    }

    public record ChatRequest(String message, Selection selection) {}

    public record Selection(List<String> satellites, List<String> stations) {}

    public record ChatResponse(String answer, List<ActionCollector.Action> actions) {}
}
