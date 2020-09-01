package net.zileo.logback.logdna;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class LogDnaAppenderDecorator extends LogDnaAppender {

    private Throwable exception;

    private Response response;

    private LogDnaResponse logDnaResponse;

    @Override
    protected Response callIngestApi(String jsonData) {

        try {

            this.response = super.callIngestApi(jsonData);
            return response;

        } catch (Throwable t) {

            this.exception = t;
            throw t;

        }

    }

    @Override
    protected LogDnaResponse convertResponseToObject(Response response) throws JsonProcessingException, JsonMappingException {
        this.logDnaResponse = super.convertResponseToObject(response);
        return this.logDnaResponse;
    }

    public boolean hasException() {
        return this.exception != null;
    }

    public boolean hasError() {
        return this.logDnaResponse != null && this.logDnaResponse.getError() != null;
    }

    public boolean isOK() {
        return this.response != null && this.response.getStatus() == 200;
    }

    public Throwable getException() {
        return exception;
    }

    public Response getResponse() {
        return response;
    }

    public LogDnaResponse getLogDnaResponse() {
        return logDnaResponse;
    }

}
