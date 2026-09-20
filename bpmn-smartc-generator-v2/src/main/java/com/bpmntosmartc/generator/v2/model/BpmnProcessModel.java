package com.bpmntosmartc.generator.v2.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BpmnProcessModel {
    private final String processId;
    private final String processName;
    private final List<BpmnNode> nodes;
    private final List<BpmnFlow> flows;
    private final List<String> unsupportedElements;
    private final Map<String, BpmnNode> nodeById;
    private final Map<String, List<BpmnFlow>> outgoingByNodeId;
    private final Map<String, List<BpmnFlow>> incomingByNodeId;

    public BpmnProcessModel(
            final String processId,
            final String processName,
            final List<BpmnNode> nodes,
            final List<BpmnFlow> flows,
            final List<String> unsupportedElements) {
        this.processId = processId;
        this.processName = processName;
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.flows = Collections.unmodifiableList(new ArrayList<>(flows));
        this.unsupportedElements = Collections.unmodifiableList(new ArrayList<>(unsupportedElements));

        this.nodeById = new HashMap<>();
        for (BpmnNode node : this.nodes) {
            this.nodeById.put(node.getId(), node);
        }

        this.outgoingByNodeId = new HashMap<>();
        this.incomingByNodeId = new HashMap<>();
        for (BpmnFlow flow : this.flows) {
            this.outgoingByNodeId.computeIfAbsent(flow.getSourceRef(), ignored -> new ArrayList<>()).add(flow);
            this.incomingByNodeId.computeIfAbsent(flow.getTargetRef(), ignored -> new ArrayList<>()).add(flow);
        }
    }

    public String getProcessId() {
        return processId;
    }

    public String getProcessName() {
        return processName;
    }

    public List<BpmnNode> getNodes() {
        return nodes;
    }

    public List<BpmnFlow> getFlows() {
        return flows;
    }

    public List<String> getUnsupportedElements() {
        return unsupportedElements;
    }

    public BpmnNode getNode(final String nodeId) {
        return nodeById.get(nodeId);
    }

    public List<BpmnFlow> outgoing(final String nodeId) {
        return outgoingByNodeId.getOrDefault(nodeId, List.of());
    }

    public List<BpmnFlow> incoming(final String nodeId) {
        return incomingByNodeId.getOrDefault(nodeId, List.of());
    }

    public List<BpmnNode> findNodes(final BpmnNodeType type) {
        return nodes.stream().filter(n -> n.getType() == type).collect(Collectors.toList());
    }
}
