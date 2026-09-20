package com.bpmntosmartc.generator.v2.model;

public class BpmnFlow {
    private final String id;
    private final String sourceRef;
    private final String targetRef;
    private final String name;
    private final String conditionExpression;

    public BpmnFlow(
            final String id,
            final String sourceRef,
            final String targetRef,
            final String name,
            final String conditionExpression) {
        this.id = id;
        this.sourceRef = sourceRef;
        this.targetRef = targetRef;
        this.name = name;
        this.conditionExpression = conditionExpression;
    }

    public String getId() {
        return id;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public String getName() {
        return name;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }
}
