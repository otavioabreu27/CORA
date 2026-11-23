package inpe.ccs.cora.api;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GrpcExceptionMapper implements ExceptionMapper<StatusRuntimeException> {

    @Override
    public Response toResponse(StatusRuntimeException exception) {
        var status = exception.getStatus();
        int httpStatus = switch (status.getCode()) {
            case INVALID_ARGUMENT, FAILED_PRECONDITION, OUT_OF_RANGE -> 400;
            case NOT_FOUND -> 404;
            case ALREADY_EXISTS, ABORTED -> 409;
            case PERMISSION_DENIED -> 403;
            case UNAUTHENTICATED -> 401;
            case RESOURCE_EXHAUSTED -> 429;
            case DEADLINE_EXCEEDED -> 504;
            case UNAVAILABLE -> 503;
            default -> 502;
        };
        String message = status.getDescription() == null
                ? status.getCode().name()
                : status.getDescription();
        return Response.status(httpStatus)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(status.getCode().name(), message))
                .build();
    }

    public record ErrorResponse(String error, String message) {}
}
