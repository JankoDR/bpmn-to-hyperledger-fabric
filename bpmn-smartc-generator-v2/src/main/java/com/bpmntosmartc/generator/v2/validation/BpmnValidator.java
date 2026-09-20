package com.bpmntosmartc.generator.v2.validation;

import com.bpmntosmartc.generator.v2.model.BpmnFlow;
import com.bpmntosmartc.generator.v2.model.BpmnNode;
import com.bpmntosmartc.generator.v2.model.BpmnNodeType;
import com.bpmntosmartc.generator.v2.model.BpmnProcessModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BpmnValidator {
    private static final Pattern COND_TRUE = Pattern.compile("^=?\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*$");
    private static final Pattern COND_FALSE = Pattern.compile("^=?\\s*not\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\)\\s*$");

    public void validate(final BpmnProcessModel model) {
        List<String> issues = new ArrayList<>();

        if (!model.getUnsupportedElements().isEmpty()) {
            issues.add("Unsupported process elements detected: " + String.join(", ", model.getUnsupportedElements()));
        }

        if (model.getNodes().isEmpty()) {
            issues.add("No supported BPMN nodes found.");
        }

        if (model.findNodes(BpmnNodeType.START_EVENT).size() != 1) {
            issues.add("Exactly one startEvent is required.");
        }

        if (model.findNodes(BpmnNodeType.END_EVENT).isEmpty()) {
            issues.add("At least one endEvent is required.");
        }

        Set<String> nodeIds = new HashSet<>();
        for (BpmnNode node : model.getNodes()) {
            if (node.getId() == null || node.getId().isBlank()) {
                issues.add("Each BPMN node must have a non-empty id.");
                continue;
            }
            if (!nodeIds.add(node.getId())) {
                issues.add("Duplicate BPMN node id: " + node.getId());
            }
        }

        Set<String> flowIds = new HashSet<>();
        for (BpmnFlow flow : model.getFlows()) {
            if (flow.getId() == null || flow.getId().isBlank()) {
                issues.add("Each sequenceFlow must have a non-empty id.");
            } else if (!flowIds.add(flow.getId())) {
                issues.add("Duplicate sequenceFlow id: " + flow.getId());
            }

            if (flow.getSourceRef() == null || flow.getSourceRef().isBlank()) {
                issues.add("sequenceFlow " + flow.getId() + " has empty sourceRef.");
            }
            if (flow.getTargetRef() == null || flow.getTargetRef().isBlank()) {
                issues.add("sequenceFlow " + flow.getId() + " has empty targetRef.");
            }

            if (model.getNode(flow.getSourceRef()) == null) {
                issues.add("sequenceFlow " + flow.getId() + " sourceRef not found: " + flow.getSourceRef());
            }
            if (model.getNode(flow.getTargetRef()) == null) {
                issues.add("sequenceFlow " + flow.getId() + " targetRef not found: " + flow.getTargetRef());
            }
        }

        for (BpmnNode node : model.getNodes()) {
            List<BpmnFlow> incoming = model.incoming(node.getId());
            List<BpmnFlow> outgoing = model.outgoing(node.getId());

            if (node.getType() == BpmnNodeType.START_EVENT) {
                if (!incoming.isEmpty()) {
                    issues.add("startEvent must not have incoming sequence flows: " + node.getId());
                }
                if (outgoing.isEmpty()) {
                    issues.add("startEvent must have at least one outgoing sequence flow: " + node.getId());
                }
            }

            if (node.getType() == BpmnNodeType.END_EVENT) {
                if (!outgoing.isEmpty()) {
                    issues.add("endEvent must not have outgoing sequence flows: " + node.getId());
                }
                if (incoming.isEmpty()) {
                    issues.add("endEvent must have at least one incoming sequence flow: " + node.getId());
                }
            }

            if (node.getType() == BpmnNodeType.PARALLEL_GATEWAY) {
                boolean split = incoming.size() == 1 && outgoing.size() >= 2;
                boolean join = incoming.size() >= 2 && outgoing.size() == 1;
                if (!split && !join) {
                    issues.add(
                            "parallelGateway must be split (1 in, N out) or join (N in, 1 out): "
                                    + node.getId()
                                    + " (in=" + incoming.size() + ", out=" + outgoing.size() + ")");
                }
            }

            if (node.getType() == BpmnNodeType.EXCLUSIVE_GATEWAY) {
                if (incoming.isEmpty() || outgoing.isEmpty()) {
                    issues.add("exclusiveGateway must have incoming and outgoing sequence flows: " + node.getId());
                }

                String defaultFlowId = node.getDefaultFlowId();
                if (defaultFlowId != null) {
                    boolean found = outgoing.stream().anyMatch(f -> defaultFlowId.equals(f.getId()));
                    if (!found) {
                        issues.add("exclusiveGateway default flow does not belong to outgoing flows: "
                                + node.getId() + " default=" + defaultFlowId);
                    }
                }

                for (BpmnFlow flow : outgoing) {
                    String condition = blankToNull(flow.getConditionExpression());
                    if (condition != null && !isSupportedCondition(condition)) {
                        issues.add("Unsupported condition expression on flow " + flow.getId() + ": " + condition
                                + ". Supported formats: =var, var, =not(var), not(var)");
                    }
                }
            }
        }

        if (!issues.isEmpty()) {
            throw new IllegalArgumentException("BPMN validation failed:\n - " + String.join("\n - ", issues));
        }
    }

    private static boolean isSupportedCondition(final String conditionExpression) {
        Matcher mTrue = COND_TRUE.matcher(conditionExpression);
        if (mTrue.matches()) {
            return true;
        }
        Matcher mFalse = COND_FALSE.matcher(conditionExpression);
        return mFalse.matches();
    }

    private static String blankToNull(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
