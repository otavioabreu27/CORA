package org.orbitalsuite.cora.api;

import io.smallrye.mutiny.TimeoutException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GrpcTimeoutExceptionMapper implements ExceptionMapper<TimeoutException> {

    @Override
    public Response toResponse(TimeoutException exception) {
        return Response.status(Response.Status.GATEWAY_TIMEOUT)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new GrpcExceptionMapper.ErrorResponse("DEADLINE_EXCEEDED", "orbital-dynamics timed out"))
                .build();
    }
}
