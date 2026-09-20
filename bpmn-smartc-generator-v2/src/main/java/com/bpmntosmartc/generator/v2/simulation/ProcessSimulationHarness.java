package com.bpmntosmartc.generator.v2.simulation;

import com.bpmntosmartc.generator.v2.model.BpmnFlow;
import com.bpmntosmartc.generator.v2.model.BpmnNode;
import com.bpmntosmartc.generator.v2.model.BpmnNodeType;
import com.bpmntosmartc.generator.v2.model.BpmnProcessModel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessSimulationHarness {
    private static final Pattern COND_TRUE = Pattern.compile("^=?\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*$");
    private static final Pattern COND_FALSE = Pattern.compile("^=?\\s*not\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\)\\s*$");

    public List<String> runDefaultScenarios(final BpmnProcessModel model, final String modelFileName) {
        List<String> output = new ArrayList<>();
        output.add("Simulation harness for " + modelFileName + ":");

        if (!isKredit05LikeModel(model)) {
            output.add("  Skipped: no predefined scenario for this model.");
            return output;
        }

        output.addAll(runKredit05HappyPath(model));
        output.addAll(runKredit05ReworkAndReject(model));
        return output;
    }

    private List<String> runKredit05HappyPath(final BpmnProcessModel model) {
        List<String> lines = new ArrayList<>();
        lines.add("  Scenario A: happy path to approved end");

        Engine engine = new Engine(model);
        engine.start();
        lines.add("    Start -> active=" + engine.activeNodeIds());

        step(lines, engine, "T01", Map.of());
        step(lines, engine, "T02", Map.of("docsComplete", "true"));
        step(lines, engine, "T05A", Map.of());
        step(lines, engine, "T05B", Map.of());
        step(lines, engine, "T05C", Map.of("eligible", "true"));
        step(lines, engine, "T06", Map.of());
        step(lines, engine, "T07", Map.of());
        step(lines, engine, "T08", Map.of("offerAccepted", "true"));
        step(lines, engine, "T09", Map.of());
        step(lines, engine, "T10", Map.of("approved", "true"));
        step(lines, engine, "T11", Map.of());
        step(lines, engine, "T12", Map.of());
        step(lines, engine, "T13", Map.of("collateralOk", "true"));
        step(lines, engine, "T14", Map.of());

        lines.add("    Ended=" + engine.isEnded() + ", active=" + engine.activeNodeIds());
        lines.add("");
        return lines;
    }

    private List<String> runKredit05ReworkAndReject(final BpmnProcessModel model) {
        List<String> lines = new ArrayList<>();
        lines.add("  Scenario B: document rework then reject");

        Engine engine = new Engine(model);
        engine.start();
        lines.add("    Start -> active=" + engine.activeNodeIds());

        step(lines, engine, "T01", Map.of());
        step(lines, engine, "T02", Map.of("docsComplete", "false"));
        step(lines, engine, "T03", Map.of());
        step(lines, engine, "T04", Map.of());
        step(lines, engine, "T02", Map.of("docsComplete", "true"));
        step(lines, engine, "T05A", Map.of());
        step(lines, engine, "T05B", Map.of());
        step(lines, engine, "T05C", Map.of("eligible", "false"));

        lines.add("    Ended=" + engine.isEnded() + ", active=" + engine.activeNodeIds());
        lines.add("");
        return lines;
    }

    private static void step(
            final List<String> lines,
            final Engine engine,
            final String taskId,
            final Map<String, String> metadataPatch) {
        engine.completeTask(taskId, metadataPatch);
        lines.add("    After " + taskId + " -> active=" + engine.activeNodeIds() + ", metadata=" + engine.metadata());
    }

    private static boolean isKredit05LikeModel(final BpmnProcessModel model) {
        Set<String> taskIds = new LinkedHashSet<>();
        for (BpmnNode node : model.findNodes(BpmnNodeType.TASK)) {
            taskIds.add(node.getId());
        }

        return taskIds.containsAll(Set.of("T01", "T02", "T05A", "T05B", "T05C", "T13", "T14"));
    }

    private static class Engine {
        private final BpmnProcessModel model;
        private final Map<String, List<String>> joinArrivals = new LinkedHashMap<>();
        private final Map<String, String> metadata = new LinkedHashMap<>();
        private final Set<String> activeNodeIds = new LinkedHashSet<>();

        private Engine(final BpmnProcessModel model) {
            this.model = model;
        }

        private void start() {
            BpmnNode start = model.findNodes(BpmnNodeType.START_EVENT).get(0);
            ArrayDeque<Transition> queue = new ArrayDeque<>();
            queue.add(new Transition("__INIT__", start.getId()));
            processQueue(queue);
        }

        private void completeTask(final String taskId, final Map<String, String> metadataPatch) {
            BpmnNode task = requireNode(taskId);
            if (task.getType() != BpmnNodeType.TASK) {
                throw new IllegalStateException("Node is not task: " + taskId);
            }
            if (!activeNodeIds.contains(taskId)) {
                throw new IllegalStateException("Task not enabled: " + taskId + " enabled=" + activeNodeIds);
            }

            activeNodeIds.remove(taskId);
            if (!metadataPatch.isEmpty()) {
                metadata.putAll(metadataPatch);
            }

            ArrayDeque<Transition> queue = new ArrayDeque<>();
            for (BpmnFlow flow : model.outgoing(taskId)) {
                queue.add(new Transition(taskId, flow.getTargetRef()));
            }
            processQueue(queue);
        }

        private void processQueue(final ArrayDeque<Transition> queue) {
            while (!queue.isEmpty()) {
                Transition transition = queue.removeFirst();
                BpmnNode target = requireNode(transition.targetNodeId);

                switch (target.getType()) {
                    case START_EVENT:
                        followAllOutgoing(target.getId(), queue);
                        break;
                    case TASK:
                    case END_EVENT:
                        activeNodeIds.add(target.getId());
                        break;
                    case PARALLEL_GATEWAY:
                        handleParallelGateway(transition, target, queue);
                        break;
                    case EXCLUSIVE_GATEWAY:
                        handleExclusiveGateway(target, queue);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported runtime node type: " + target.getType());
                }
            }
        }

        private void handleParallelGateway(
                final Transition transition,
                final BpmnNode gateway,
                final ArrayDeque<Transition> queue) {
            List<BpmnFlow> incoming = model.incoming(gateway.getId());
            List<BpmnFlow> outgoing = model.outgoing(gateway.getId());

            boolean split = incoming.size() == 1 && outgoing.size() >= 2;
            boolean join = incoming.size() >= 2 && outgoing.size() == 1;

            if (split) {
                followAllOutgoing(gateway.getId(), queue);
                return;
            }

            if (join) {
                List<String> arrivals = joinArrivals.computeIfAbsent(gateway.getId(), ignored -> new ArrayList<>());
                if (!arrivals.contains(transition.sourceNodeId)) {
                    arrivals.add(transition.sourceNodeId);
                }

                Set<String> requiredIncomingNodes = new LinkedHashSet<>();
                for (BpmnFlow flow : incoming) {
                    requiredIncomingNodes.add(flow.getSourceRef());
                }

                if (arrivals.containsAll(requiredIncomingNodes)) {
                    joinArrivals.remove(gateway.getId());
                    queue.add(new Transition(gateway.getId(), outgoing.get(0).getTargetRef()));
                }
                return;
            }

            throw new IllegalStateException("Invalid parallel gateway shape: " + gateway.getId());
        }

        private void handleExclusiveGateway(final BpmnNode gateway, final ArrayDeque<Transition> queue) {
            List<BpmnFlow> outgoing = model.outgoing(gateway.getId());
            String selectedFlowId = null;

            for (BpmnFlow flow : outgoing) {
                ConditionDescriptor descriptor = parseCondition(flow.getConditionExpression());
                if (descriptor == null) {
                    continue;
                }

                boolean actual = parseBoolean(metadata.get(descriptor.variableName));
                if (actual == descriptor.expectedValue) {
                    if (selectedFlowId != null) {
                        throw new IllegalStateException("Ambiguous exclusive routing at " + gateway.getId());
                    }
                    selectedFlowId = flow.getId();
                }
            }

            if (selectedFlowId == null && gateway.getDefaultFlowId() != null) {
                selectedFlowId = gateway.getDefaultFlowId();
            }

            if (selectedFlowId == null && outgoing.size() == 1) {
                selectedFlowId = outgoing.get(0).getId();
            }

            if (selectedFlowId == null) {
                throw new IllegalStateException("No route selected at exclusive gateway " + gateway.getId());
            }

            for (BpmnFlow flow : outgoing) {
                if (flow.getId().equals(selectedFlowId)) {
                    queue.add(new Transition(gateway.getId(), flow.getTargetRef()));
                    return;
                }
            }

            throw new IllegalStateException("Selected flow not found on gateway " + gateway.getId() + ": " + selectedFlowId);
        }

        private void followAllOutgoing(final String sourceNodeId, final ArrayDeque<Transition> queue) {
            for (BpmnFlow flow : model.outgoing(sourceNodeId)) {
                queue.add(new Transition(sourceNodeId, flow.getTargetRef()));
            }
        }

        private BpmnNode requireNode(final String nodeId) {
            BpmnNode node = model.getNode(nodeId);
            if (node == null) {
                throw new IllegalStateException("Missing node: " + nodeId);
            }
            return node;
        }

        private boolean isEnded() {
            if (activeNodeIds.isEmpty()) {
                return false;
            }
            for (String nodeId : activeNodeIds) {
                BpmnNode node = requireNode(nodeId);
                if (node.getType() != BpmnNodeType.END_EVENT) {
                    return false;
                }
            }
            return joinArrivals.isEmpty();
        }

        private List<String> activeNodeIds() {
            return new ArrayList<>(activeNodeIds);
        }

        private Map<String, String> metadata() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        }
    }

    private static ConditionDescriptor parseCondition(final String conditionExpression) {
        if (conditionExpression == null || conditionExpression.isBlank()) {
            return null;
        }

        String expression = conditionExpression.trim();
        Matcher trueMatch = COND_TRUE.matcher(expression);
        if (trueMatch.matches()) {
            return new ConditionDescriptor(trueMatch.group(1), true);
        }

        Matcher falseMatch = COND_FALSE.matcher(expression);
        if (falseMatch.matches()) {
            return new ConditionDescriptor(falseMatch.group(1), false);
        }

        return null;
    }

    private static boolean parseBoolean(final String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
    }

    private static class ConditionDescriptor {
        private final String variableName;
        private final boolean expectedValue;

        private ConditionDescriptor(final String variableName, final boolean expectedValue) {
            this.variableName = variableName;
            this.expectedValue = expectedValue;
        }
    }

    private static class Transition {
        private final String sourceNodeId;
        private final String targetNodeId;

        private Transition(final String sourceNodeId, final String targetNodeId) {
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
        }
    }
}
