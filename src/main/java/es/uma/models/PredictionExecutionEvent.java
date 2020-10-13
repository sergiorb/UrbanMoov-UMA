package es.uma.models;

public class PredictionExecutionEvent {
    public String id;
    public String timestamp;
    public String name;
    public String version;
    public Boolean ack;
    public String etlId;
    public String executionId;
    public String userId;
    public Error error;
}
