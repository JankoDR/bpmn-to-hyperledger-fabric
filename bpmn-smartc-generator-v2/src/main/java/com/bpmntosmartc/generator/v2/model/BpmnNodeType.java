package com.bpmntosmartc.generator.v2.model;

public enum BpmnNodeType {
    START_EVENT,
    END_EVENT,
    TASK,
    EXCLUSIVE_GATEWAY,
    PARALLEL_GATEWAY;

    public static BpmnNodeType fromLocalName(final String localName) {
        String value = localName == null ? "" : localName.trim();
        if (value.equals("startEvent")) {
            return START_EVENT;
        }
        if (value.equals("endEvent")) {
            return END_EVENT;
        }
        if (value.equals("exclusiveGateway")) {
            return EXCLUSIVE_GATEWAY;
        }
        if (value.equals("parallelGateway")) {
            return PARALLEL_GATEWAY;
        }
        if (value.equals("task") || value.endsWith("Task")) {
            return TASK;
        }
        return null;
    }
}
