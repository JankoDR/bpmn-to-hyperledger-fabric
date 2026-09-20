package com.bpmntosmartc.generator.v2.model;

public class BpmnNode {
    private final String id;
    private final String name;
    private final BpmnNodeType type;
    private final String defaultFlowId;

    public BpmnNode(final String id, final String name, final BpmnNodeType type, final String defaultFlowId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.defaultFlowId = defaultFlowId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BpmnNodeType getType() {
        return type;
    }

    public String getDefaultFlowId() {
        return defaultFlowId;
    }

    public String displayName() {
        if (name == null || name.isBlank()) {
            return id;
        }
        return name;
    }
}
