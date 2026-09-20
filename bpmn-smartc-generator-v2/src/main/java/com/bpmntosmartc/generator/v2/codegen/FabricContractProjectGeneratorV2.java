package com.bpmntosmartc.generator.v2.codegen;

import com.bpmntosmartc.generator.v2.model.BpmnFlow;
import com.bpmntosmartc.generator.v2.model.BpmnNode;
import com.bpmntosmartc.generator.v2.model.BpmnNodeType;
import com.bpmntosmartc.generator.v2.model.BpmnProcessModel;
import com.bpmntosmartc.generator.v2.util.NameUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FabricContractProjectGeneratorV2 {
    private static final Pattern COND_TRUE = Pattern.compile("^=?\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*$");
    private static final Pattern COND_FALSE = Pattern.compile("^=?\\s*not\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\)\\s*$");

    public GeneratedContractInfo generate(
            final BpmnProcessModel model,
            final Path outputProjectDir,
            final String basePackage,
            final String packageToken,
            final String artifactId) throws IOException {

        final String packageName = basePackage + "." + packageToken;
        final String contractClass = NameUtils.toClassName(model.getProcessName()) + "Contract";

        final Path srcRoot = outputProjectDir.resolve("src/main/java");
        final Path packageDir = srcRoot.resolve(packageName.replace('.', '/'));
        final Path testRoot = outputProjectDir.resolve("src/test/java");
        final Path testPackageDir = testRoot.resolve(packageName.replace('.', '/'));
        Files.createDirectories(packageDir);
        Files.createDirectories(testPackageDir);

        final Path contractFile = packageDir.resolve(contractClass + ".java");
        final Path stateFile = packageDir.resolve("ProcessState.java");

        Files.writeString(contractFile, renderContract(packageName, contractClass, model));
        Files.writeString(stateFile, renderProcessState(packageName));
        Files.writeString(testPackageDir.resolve(contractClass + "CreationTest.java"),
            renderCreationTest(packageName, contractClass));
        Files.writeString(testPackageDir.resolve(contractClass + "ReadTest.java"),
            renderReadTest(packageName, contractClass));
        Files.writeString(testPackageDir.resolve(contractClass + "ValidationTest.java"),
            renderValidationTest(packageName, contractClass));
        Files.writeString(outputProjectDir.resolve("pom.xml"), renderPom(artifactId));
        Files.writeString(outputProjectDir.resolve("build.gradle"), renderGradleBuild(packageToken));
        Files.writeString(outputProjectDir.resolve("settings.gradle"), renderGradleSettings(artifactId));
        copyGradleWrapper(outputProjectDir);
        Files.writeString(outputProjectDir.resolve("README.md"), renderReadme(packageName, contractClass, model, artifactId));

        return new GeneratedContractInfo(outputProjectDir, packageName, contractClass);
    }

    private String renderContract(final String packageName, final String contractClass, final BpmnProcessModel model) {
        String startEventId = model.findNodes(BpmnNodeType.START_EVENT).get(0).getId();

        StringBuilder sb = new StringBuilder();
        line(sb, "package " + packageName + ";");
        line(sb, "");
        line(sb, "import com.owlike.genson.Genson;");
        line(sb, "import com.owlike.genson.GenericType;");
        line(sb, "import java.util.ArrayDeque;");
        line(sb, "import java.util.ArrayList;");
        line(sb, "import java.util.Collections;");
        line(sb, "import java.util.HashMap;");
        line(sb, "import java.util.LinkedHashMap;");
        line(sb, "import java.util.LinkedHashSet;");
        line(sb, "import java.util.List;");
        line(sb, "import java.util.Map;");
        line(sb, "import java.util.Set;");
        line(sb, "import org.hyperledger.fabric.contract.Context;");
        line(sb, "import org.hyperledger.fabric.contract.ContractInterface;");
        line(sb, "import org.hyperledger.fabric.contract.annotation.Contract;");
        line(sb, "import org.hyperledger.fabric.contract.annotation.Default;");
        line(sb, "import org.hyperledger.fabric.contract.annotation.Info;");
        line(sb, "import org.hyperledger.fabric.contract.annotation.Transaction;");
        line(sb, "import org.hyperledger.fabric.shim.ChaincodeException;");
        line(sb, "");
        line(sb, "@Contract(name = " + NameUtils.javaStringLiteral(model.getProcessName()) + ", info = @Info(");
        line(sb, "        title = " + NameUtils.javaStringLiteral(model.getProcessName()) + ",");
        line(sb, "        description = " + NameUtils.javaStringLiteral("Generated from BPMN process " + model.getProcessId()) + ",");
        line(sb, "        version = \"2.0.0\"))");
        line(sb, "@Default");
        line(sb, "public class " + contractClass + " implements ContractInterface {");
        line(sb, "    private static final Genson GENSON = new Genson();");
        line(sb, "    private static final GenericType<Map<String, String>> MAP_TYPE = new GenericType<Map<String, String>>() {};");
        line(sb, "    private static final String PROCESS_KEY_PREFIX = \"PROC_V2_\";");
        line(sb, "    private static final String START_EVENT_ID = " + NameUtils.javaStringLiteral(startEventId) + ";");
        line(sb, "");

        line(sb, "    private static final Map<String, String> NODE_TYPE = new LinkedHashMap<>();");
        line(sb, "    private static final Map<String, String> NODE_LABEL = new LinkedHashMap<>();");
        line(sb, "    private static final Set<String> TASK_IDS = new LinkedHashSet<>();");
        line(sb, "    private static final Set<String> END_EVENT_IDS = new LinkedHashSet<>();");
        line(sb, "    private static final Map<String, List<String>> OUTGOING_FLOW_IDS = new LinkedHashMap<>();");
        line(sb, "    private static final Map<String, List<String>> INCOMING_NODE_IDS = new LinkedHashMap<>();");
        line(sb, "    private static final Map<String, String> FLOW_TARGET = new LinkedHashMap<>();");
        line(sb, "    private static final Map<String, String> FLOW_CONDITION_VAR = new LinkedHashMap<>();");
        line(sb, "    private static final Map<String, String> FLOW_CONDITION_EXPECTED = new LinkedHashMap<>();");
        line(sb, "    private static final Map<String, String> EXCLUSIVE_DEFAULT_FLOW = new LinkedHashMap<>();");
        line(sb, "");

        line(sb, "    static {");
        for (BpmnNode node : model.getNodes()) {
            line(sb, "        NODE_TYPE.put(" + NameUtils.javaStringLiteral(node.getId()) + ", "
                    + NameUtils.javaStringLiteral(typeToken(node.getType())) + ");");
            line(sb, "        NODE_LABEL.put(" + NameUtils.javaStringLiteral(node.getId()) + ", "
                    + NameUtils.javaStringLiteral(node.displayName()) + ");");
            if (node.getType() == BpmnNodeType.TASK) {
                line(sb, "        TASK_IDS.add(" + NameUtils.javaStringLiteral(node.getId()) + ");");
            }
            if (node.getType() == BpmnNodeType.END_EVENT) {
                line(sb, "        END_EVENT_IDS.add(" + NameUtils.javaStringLiteral(node.getId()) + ");");
            }
            if (node.getType() == BpmnNodeType.EXCLUSIVE_GATEWAY && node.getDefaultFlowId() != null) {
                line(sb, "        EXCLUSIVE_DEFAULT_FLOW.put(" + NameUtils.javaStringLiteral(node.getId()) + ", "
                        + NameUtils.javaStringLiteral(node.getDefaultFlowId()) + ");");
            }
        }

        for (BpmnFlow flow : model.getFlows()) {
            line(sb, "        OUTGOING_FLOW_IDS.computeIfAbsent(" + NameUtils.javaStringLiteral(flow.getSourceRef())
                    + ", k -> new ArrayList<>()).add(" + NameUtils.javaStringLiteral(flow.getId()) + ");");
            line(sb, "        INCOMING_NODE_IDS.computeIfAbsent(" + NameUtils.javaStringLiteral(flow.getTargetRef())
                    + ", k -> new ArrayList<>()).add(" + NameUtils.javaStringLiteral(flow.getSourceRef()) + ");");
            line(sb, "        FLOW_TARGET.put(" + NameUtils.javaStringLiteral(flow.getId()) + ", "
                    + NameUtils.javaStringLiteral(flow.getTargetRef()) + ");");

            ConditionDescriptor condition = parseCondition(flow.getConditionExpression());
            if (condition != null) {
                line(sb, "        FLOW_CONDITION_VAR.put(" + NameUtils.javaStringLiteral(flow.getId()) + ", "
                        + NameUtils.javaStringLiteral(condition.variableName) + ");");
                line(sb, "        FLOW_CONDITION_EXPECTED.put(" + NameUtils.javaStringLiteral(flow.getId()) + ", "
                        + NameUtils.javaStringLiteral(Boolean.toString(condition.expectedValue)) + ");");
            }
        }
        line(sb, "    }");
        line(sb, "");

        line(sb, "    @Transaction(intent = Transaction.TYPE.SUBMIT)");
        line(sb, "    public ProcessState CreateProcess(final Context ctx, final String processId) {");
        line(sb, "        String key = stateKey(processId);");
        line(sb, "        String existing = ctx.getStub().getStringState(key);");
        line(sb, "        if (existing != null && !existing.isEmpty()) {");
        line(sb, "            throw new ChaincodeException(\"Process already exists: \" + processId);");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        ProcessState state = new ProcessState();");
        line(sb, "        state.setProcessId(processId);");
        line(sb, "");
        line(sb, "        ArrayDeque<Transition> queue = new ArrayDeque<>();");
        line(sb, "        queue.add(new Transition(\"__INIT__\", START_EVENT_ID));");
        line(sb, "        processQueue(state, queue);");
        line(sb, "        refreshEndedFlag(state);");
        line(sb, "        saveState(ctx, state);");
        line(sb, "        return state;");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    @Transaction(intent = Transaction.TYPE.EVALUATE)");
        line(sb, "    public ProcessState ReadProcess(final Context ctx, final String processId) {");
        line(sb, "        return loadState(ctx, processId);");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    @Transaction(intent = Transaction.TYPE.SUBMIT)");
        line(sb, "    public ProcessState CompleteActivity(");
        line(sb, "            final Context ctx,");
        line(sb, "            final String processId,");
        line(sb, "            final String activityId,");
        line(sb, "            final String metadataJson) {");
        line(sb, "        return completeTask(ctx, processId, activityId, metadataJson);");
        line(sb, "    }");
        line(sb, "");

        Set<String> usedMethods = new HashSet<>();
        for (BpmnNode task : model.findNodes(BpmnNodeType.TASK)) {
            String base = "complete" + NameUtils.toClassName(task.getId() + "_" + task.displayName());
            String methodName = uniqueMethodName(base, usedMethods);
            line(sb, "    @Transaction(intent = Transaction.TYPE.SUBMIT)");
            line(sb, "    public ProcessState " + methodName + "(");
            line(sb, "            final Context ctx,");
            line(sb, "            final String processId,");
            line(sb, "            final String metadataJson) {");
            line(sb, "        return completeTask(ctx, processId, " + NameUtils.javaStringLiteral(task.getId()) + ", metadataJson);");
            line(sb, "    }");
            line(sb, "");
        }

        line(sb, "    @Transaction(intent = Transaction.TYPE.EVALUATE)");
        line(sb, "    public String GetEnabledActivities(final Context ctx, final String processId) {");
        line(sb, "        ProcessState state = loadState(ctx, processId);");
        line(sb, "        List<String> enabled = new ArrayList<>();");
        line(sb, "        for (String activeNodeId : state.getActiveNodeIds()) {");
        line(sb, "            if (TASK_IDS.contains(activeNodeId)) {");
        line(sb, "                enabled.add(activeNodeId);");
        line(sb, "            }");
        line(sb, "        }");
        line(sb, "        return GENSON.serialize(enabled);");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    @Transaction(intent = Transaction.TYPE.EVALUATE)");
        line(sb, "    public boolean IsAtEnd(final Context ctx, final String processId) {");
        line(sb, "        ProcessState state = loadState(ctx, processId);");
        line(sb, "        return state.isEnded();");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    @Transaction(intent = Transaction.TYPE.EVALUATE)");
        line(sb, "    public String GetNodeCatalog(final Context ctx) {");
        line(sb, "        return GENSON.serialize(NODE_LABEL);");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private ProcessState completeTask(");
        line(sb, "            final Context ctx,");
        line(sb, "            final String processId,");
        line(sb, "            final String taskId,");
        line(sb, "            final String metadataJson) {");
        line(sb, "        ProcessState state = loadState(ctx, processId);");
        line(sb, "        requireTaskNode(taskId);");
        line(sb, "        if (!state.getActiveNodeIds().contains(taskId)) {");
        line(sb, "            throw new ChaincodeException(\"Task is not enabled: \" + taskId);");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        Map<String, String> metadataPatch = parseMetadata(metadataJson);");
        line(sb, "        if (!metadataPatch.isEmpty()) {");
        line(sb, "            state.getMetadata().putAll(metadataPatch);");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        state.getActiveNodeIds().remove(taskId);");
        line(sb, "        if (!state.getCompletedTaskIds().contains(taskId)) {");
        line(sb, "            state.getCompletedTaskIds().add(taskId);");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        ArrayDeque<Transition> queue = new ArrayDeque<>();");
        line(sb, "        for (String flowId : OUTGOING_FLOW_IDS.getOrDefault(taskId, Collections.emptyList())) {");
        line(sb, "            queue.add(new Transition(taskId, FLOW_TARGET.get(flowId)));");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        processQueue(state, queue);");
        line(sb, "        refreshEndedFlag(state);");
        line(sb, "        saveState(ctx, state);");
        line(sb, "        return state;");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private void processQueue(final ProcessState state, final ArrayDeque<Transition> queue) {");
        line(sb, "        while (!queue.isEmpty()) {");
        line(sb, "            Transition transition = queue.removeFirst();");
        line(sb, "            String targetNodeId = transition.targetNodeId;");
        line(sb, "            String targetType = NODE_TYPE.get(targetNodeId);");
        line(sb, "            if (targetType == null) {");
        line(sb, "                throw new ChaincodeException(\"Unknown target node: \" + targetNodeId);");
        line(sb, "            }");
        line(sb, "");
        line(sb, "            switch (targetType) {");
        line(sb, "                case \"startEvent\":");
        line(sb, "                    followAllOutgoing(targetNodeId, queue);");
        line(sb, "                    break;");
        line(sb, "                case \"task\":");
        line(sb, "                    activateNode(state, targetNodeId);");
        line(sb, "                    break;");
        line(sb, "                case \"endEvent\":");
        line(sb, "                    activateNode(state, targetNodeId);");
        line(sb, "                    break;");
        line(sb, "                case \"parallelGateway\":");
        line(sb, "                    handleParallelGateway(state, transition, queue);");
        line(sb, "                    break;");
        line(sb, "                case \"exclusiveGateway\":");
        line(sb, "                    handleExclusiveGateway(state, targetNodeId, queue);");
        line(sb, "                    break;");
        line(sb, "                default:");
        line(sb, "                    throw new ChaincodeException(\"Unsupported node type at runtime: \" + targetType);");
        line(sb, "            }");
        line(sb, "        }");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private void handleParallelGateway(");
        line(sb, "            final ProcessState state,");
        line(sb, "            final Transition transition,");
        line(sb, "            final ArrayDeque<Transition> queue) {");
        line(sb, "        String gatewayId = transition.targetNodeId;");
        line(sb, "        List<String> incomingNodes = INCOMING_NODE_IDS.getOrDefault(gatewayId, Collections.emptyList());");
        line(sb, "        List<String> outgoingFlows = OUTGOING_FLOW_IDS.getOrDefault(gatewayId, Collections.emptyList());");
        line(sb, "");
        line(sb, "        boolean isSplit = incomingNodes.size() == 1 && outgoingFlows.size() >= 2;");
        line(sb, "        boolean isJoin = incomingNodes.size() >= 2 && outgoingFlows.size() == 1;");
        line(sb, "");
        line(sb, "        if (isSplit) {");
        line(sb, "            followAllOutgoing(gatewayId, queue);");
        line(sb, "            return;");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        if (isJoin) {");
        line(sb, "            List<String> arrivals = state.getJoinArrivals().computeIfAbsent(gatewayId, ignored -> new ArrayList<>());");
        line(sb, "            if (!arrivals.contains(transition.sourceNodeId)) {");
        line(sb, "                arrivals.add(transition.sourceNodeId);");
        line(sb, "            }");
        line(sb, "");
        line(sb, "            if (arrivals.containsAll(incomingNodes)) {");
        line(sb, "                state.getJoinArrivals().remove(gatewayId);");
        line(sb, "                String outFlow = outgoingFlows.get(0);");
        line(sb, "                queue.add(new Transition(gatewayId, FLOW_TARGET.get(outFlow)));");
        line(sb, "            }");
        line(sb, "            return;");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        throw new ChaincodeException(\"Invalid parallel gateway shape: \" + gatewayId);");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private void handleExclusiveGateway(");
        line(sb, "            final ProcessState state,");
        line(sb, "            final String gatewayId,");
        line(sb, "            final ArrayDeque<Transition> queue) {");
        line(sb, "        List<String> outgoingFlows = OUTGOING_FLOW_IDS.getOrDefault(gatewayId, Collections.emptyList());");
        line(sb, "        if (outgoingFlows.isEmpty()) {");
        line(sb, "            throw new ChaincodeException(\"exclusiveGateway has no outgoing flows: \" + gatewayId);");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        String selectedFlow = null;");
        line(sb, "        for (String flowId : outgoingFlows) {");
        line(sb, "            String variable = FLOW_CONDITION_VAR.get(flowId);");
        line(sb, "            if (variable == null) {");
        line(sb, "                continue;");
        line(sb, "            }");
        line(sb, "            boolean expected = Boolean.parseBoolean(FLOW_CONDITION_EXPECTED.get(flowId));");
        line(sb, "            boolean actual = parseBoolean(state.getMetadata().get(variable));");
        line(sb, "            if (actual == expected) {");
        line(sb, "                if (selectedFlow != null) {");
        line(sb, "                    throw new ChaincodeException(\"Ambiguous exclusive routing at gateway \" + gatewayId);");
        line(sb, "                }");
        line(sb, "                selectedFlow = flowId;");
        line(sb, "            }");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        if (selectedFlow == null) {");
        line(sb, "            String defaultFlow = EXCLUSIVE_DEFAULT_FLOW.get(gatewayId);");
        line(sb, "            if (defaultFlow != null) {");
        line(sb, "                selectedFlow = defaultFlow;");
        line(sb, "            }");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        if (selectedFlow == null && outgoingFlows.size() == 1) {");
        line(sb, "            selectedFlow = outgoingFlows.get(0);");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        if (selectedFlow == null) {");
        line(sb, "            throw new ChaincodeException(\"No route selected at exclusiveGateway \" + gatewayId"
                + " + \". Provide metadata fields required by conditions.\");");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        queue.add(new Transition(gatewayId, FLOW_TARGET.get(selectedFlow)));");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private void followAllOutgoing(final String sourceNodeId, final ArrayDeque<Transition> queue) {");
        line(sb, "        for (String flowId : OUTGOING_FLOW_IDS.getOrDefault(sourceNodeId, Collections.emptyList())) {");
        line(sb, "            queue.add(new Transition(sourceNodeId, FLOW_TARGET.get(flowId)));");
        line(sb, "        }");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private void activateNode(final ProcessState state, final String nodeId) {");
        line(sb, "        if (!state.getActiveNodeIds().contains(nodeId)) {");
        line(sb, "            state.getActiveNodeIds().add(nodeId);");
        line(sb, "        }");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private void refreshEndedFlag(final ProcessState state) {");
        line(sb, "        if (state.getActiveNodeIds().isEmpty()) {");
        line(sb, "            state.setEnded(false);");
        line(sb, "            return;");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        for (String activeNodeId : state.getActiveNodeIds()) {");
        line(sb, "            if (!END_EVENT_IDS.contains(activeNodeId)) {");
        line(sb, "                state.setEnded(false);");
        line(sb, "                return;");
        line(sb, "            }");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        state.setEnded(state.getJoinArrivals().isEmpty());");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private void requireTaskNode(final String nodeId) {");
        line(sb, "        if (!TASK_IDS.contains(nodeId)) {");
        line(sb, "            throw new ChaincodeException(\"Node is not a task/activity: \" + nodeId);");
        line(sb, "        }");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private ProcessState loadState(final Context ctx, final String processId) {");
        line(sb, "        String data = ctx.getStub().getStringState(stateKey(processId));");
        line(sb, "        if (data == null || data.isEmpty()) {");
        line(sb, "            throw new ChaincodeException(\"Process does not exist: \" + processId);");
        line(sb, "        }");
        line(sb, "");
        line(sb, "        ProcessState state = GENSON.deserialize(data, ProcessState.class);");
        line(sb, "        if (state.getActiveNodeIds() == null) {");
        line(sb, "            state.setActiveNodeIds(new ArrayList<>());");
        line(sb, "        }");
        line(sb, "        if (state.getCompletedTaskIds() == null) {");
        line(sb, "            state.setCompletedTaskIds(new ArrayList<>());");
        line(sb, "        }");
        line(sb, "        if (state.getJoinArrivals() == null) {");
        line(sb, "            state.setJoinArrivals(new HashMap<>());");
        line(sb, "        }");
        line(sb, "        if (state.getMetadata() == null) {");
        line(sb, "            state.setMetadata(new HashMap<>());");
        line(sb, "        }");
        line(sb, "        return state;");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private void saveState(final Context ctx, final ProcessState state) {");
        line(sb, "        ctx.getStub().putStringState(stateKey(state.getProcessId()), GENSON.serialize(state));");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private static String stateKey(final String processId) {");
        line(sb, "        return PROCESS_KEY_PREFIX + processId;");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private Map<String, String> parseMetadata(final String metadataJson) {");
        line(sb, "        if (metadataJson == null || metadataJson.isBlank()) {");
        line(sb, "            return new HashMap<>();");
        line(sb, "        }");
        line(sb, "        try {");
        line(sb, "            return GENSON.deserialize(metadataJson, MAP_TYPE);");
        line(sb, "        } catch (Exception ex) {");
        line(sb, "            throw new ChaincodeException(\"metadataJson must be a valid JSON object of string values.\", ex);");
        line(sb, "        }");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private static boolean parseBoolean(final String value) {");
        line(sb, "        if (value == null) {");
        line(sb, "            return false;");
        line(sb, "        }");
        line(sb, "        String normalized = value.trim().toLowerCase();");
        line(sb, "        return \"true\".equals(normalized) || \"1\".equals(normalized) || \"yes\".equals(normalized);");
        line(sb, "    }");
        line(sb, "");

        line(sb, "    private static class Transition {");
        line(sb, "        private final String sourceNodeId;");
        line(sb, "        private final String targetNodeId;");
        line(sb, "");
        line(sb, "        private Transition(final String sourceNodeId, final String targetNodeId) {");
        line(sb, "            this.sourceNodeId = sourceNodeId;");
        line(sb, "            this.targetNodeId = targetNodeId;");
        line(sb, "        }");
        line(sb, "    }");

        line(sb, "}");
        return sb.toString();
    }

    private String renderProcessState(final String packageName) {
        return "package " + packageName + ";\n\n"
                + "import java.util.ArrayList;\n"
                + "import java.util.HashMap;\n"
                + "import java.util.List;\n"
                + "import java.util.Map;\n"
                + "import org.hyperledger.fabric.contract.annotation.DataType;\n"
                + "import org.hyperledger.fabric.contract.annotation.Property;\n\n"
                + "@DataType()\n"
                + "public class ProcessState {\n"
                + "    @Property()\n"
                + "    private String processId;\n"
                + "    @Property()\n"
                + "    private List<String> activeNodeIds = new ArrayList<>();\n"
                + "    @Property()\n"
                + "    private List<String> completedTaskIds = new ArrayList<>();\n"
                + "    @Property()\n"
                + "    private Map<String, List<String>> joinArrivals = new HashMap<>();\n"
                + "    @Property()\n"
                + "    private Map<String, String> metadata = new HashMap<>();\n"
                + "    @Property()\n"
                + "    private boolean ended;\n\n"
                + "    public String getProcessId() {\n"
                + "        return processId;\n"
                + "    }\n\n"
                + "    public void setProcessId(final String processId) {\n"
                + "        this.processId = processId;\n"
                + "    }\n\n"
                + "    public List<String> getActiveNodeIds() {\n"
                + "        return activeNodeIds;\n"
                + "    }\n\n"
                + "    public void setActiveNodeIds(final List<String> activeNodeIds) {\n"
                + "        this.activeNodeIds = activeNodeIds;\n"
                + "    }\n\n"
                + "    public List<String> getCompletedTaskIds() {\n"
                + "        return completedTaskIds;\n"
                + "    }\n\n"
                + "    public void setCompletedTaskIds(final List<String> completedTaskIds) {\n"
                + "        this.completedTaskIds = completedTaskIds;\n"
                + "    }\n\n"
                + "    public Map<String, List<String>> getJoinArrivals() {\n"
                + "        return joinArrivals;\n"
                + "    }\n\n"
                + "    public void setJoinArrivals(final Map<String, List<String>> joinArrivals) {\n"
                + "        this.joinArrivals = joinArrivals;\n"
                + "    }\n\n"
                + "    public Map<String, String> getMetadata() {\n"
                + "        return metadata;\n"
                + "    }\n\n"
                + "    public void setMetadata(final Map<String, String> metadata) {\n"
                + "        this.metadata = metadata;\n"
                + "    }\n\n"
                + "    public boolean isEnded() {\n"
                + "        return ended;\n"
                + "    }\n\n"
                + "    public void setEnded(final boolean ended) {\n"
                + "        this.ended = ended;\n"
                + "    }\n"
                + "}\n";
    }

    private String renderCreationTest(final String packageName, final String contractClass) {
        return "package " + packageName + ";\n\n"
                + "import static org.junit.jupiter.api.Assertions.assertEquals;\n"
                + "import static org.junit.jupiter.api.Assertions.assertFalse;\n"
                + "import static org.mockito.ArgumentMatchers.contains;\n"
                + "import static org.mockito.ArgumentMatchers.eq;\n"
                + "import static org.mockito.Mockito.mock;\n"
                + "import static org.mockito.Mockito.verify;\n"
                + "import static org.mockito.Mockito.when;\n\n"
                + "import com.google.protobuf.ByteString;\n"
                + "import org.hyperledger.fabric.contract.Context;\n"
                + "import org.hyperledger.fabric.protos.msp.SerializedIdentity;\n"
                + "import org.hyperledger.fabric.shim.ChaincodeStub;\n"
                + "import org.junit.jupiter.api.Test;\n\n"
                + "class " + contractClass + "CreationTest {\n"
                + "    private final " + contractClass + " contract = new " + contractClass + "();\n"
                + "    private final ChaincodeStub stub = stubWithCreator();\n"
                + "    private final Context ctx = new Context(stub);\n\n"
                + renderTestIdentityHelper()
                + "    @Test\n"
                + "    void createsProcessAndPersistsInitialState() {\n"
                + "        when(stub.getStringState(\"PROC_V2_case-001\")).thenReturn(\"\");\n\n"
                + "        ProcessState state = contract.CreateProcess(ctx, \"case-001\");\n\n"
                + "        assertEquals(\"case-001\", state.getProcessId());\n"
                + "        assertFalse(state.getActiveNodeIds().isEmpty());\n"
                + "        verify(stub).putStringState(eq(\"PROC_V2_case-001\"), contains(\"case-001\"));\n"
                + "    }\n"
                + "}\n";
    }

    private String renderReadTest(final String packageName, final String contractClass) {
        return "package " + packageName + ";\n\n"
                + "import static org.junit.jupiter.api.Assertions.assertEquals;\n"
                + "import static org.junit.jupiter.api.Assertions.assertFalse;\n"
                + "import static org.mockito.Mockito.mock;\n"
                + "import static org.mockito.Mockito.when;\n\n"
                + "import com.owlike.genson.Genson;\n"
                + "import com.google.protobuf.ByteString;\n"
                + "import java.util.List;\n"
                + "import org.hyperledger.fabric.contract.Context;\n"
                + "import org.hyperledger.fabric.protos.msp.SerializedIdentity;\n"
                + "import org.hyperledger.fabric.shim.ChaincodeStub;\n"
                + "import org.junit.jupiter.api.Test;\n\n"
                + "class " + contractClass + "ReadTest {\n"
                + "    private static final Genson GENSON = new Genson();\n\n"
                + "    private final " + contractClass + " contract = new " + contractClass + "();\n"
                + "    private final ChaincodeStub stub = stubWithCreator();\n"
                + "    private final Context ctx = new Context(stub);\n\n"
                + renderTestIdentityHelper()
                + "    @Test\n"
                + "    void readsPersistedProcessState() {\n"
                + "        ProcessState stored = new ProcessState();\n"
                + "        stored.setProcessId(\"case-002\");\n"
                + "        stored.getActiveNodeIds().add(\"task-a\");\n"
                + "        when(stub.getStringState(\"PROC_V2_case-002\")).thenReturn(GENSON.serialize(stored));\n\n"
                + "        ProcessState state = contract.ReadProcess(ctx, \"case-002\");\n\n"
                + "        assertEquals(\"case-002\", state.getProcessId());\n"
                + "        assertEquals(List.of(\"task-a\"), state.getActiveNodeIds());\n"
                + "        assertFalse(state.isEnded());\n"
                + "    }\n"
                + "}\n";
    }

    private String renderValidationTest(final String packageName, final String contractClass) {
        return "package " + packageName + ";\n\n"
                + "import static org.junit.jupiter.api.Assertions.assertThrows;\n"
                + "import static org.mockito.ArgumentMatchers.anyString;\n"
                + "import static org.mockito.Mockito.mock;\n"
                + "import static org.mockito.Mockito.never;\n"
                + "import static org.mockito.Mockito.verify;\n"
                + "import static org.mockito.Mockito.when;\n\n"
                + "import com.google.protobuf.ByteString;\n"
                + "import org.hyperledger.fabric.contract.Context;\n"
                + "import org.hyperledger.fabric.protos.msp.SerializedIdentity;\n"
                + "import org.hyperledger.fabric.shim.ChaincodeException;\n"
                + "import org.hyperledger.fabric.shim.ChaincodeStub;\n"
                + "import org.junit.jupiter.api.Test;\n\n"
                + "class " + contractClass + "ValidationTest {\n"
                + "    private final " + contractClass + " contract = new " + contractClass + "();\n"
                + "    private final ChaincodeStub stub = stubWithCreator();\n"
                + "    private final Context ctx = new Context(stub);\n\n"
                + renderTestIdentityHelper()
                + "    @Test\n"
                + "    void rejectsDuplicateProcessId() {\n"
                + "        when(stub.getStringState(\"PROC_V2_case-003\")).thenReturn(\"{\\\"processId\\\":\\\"case-003\\\"}\");\n\n"
                + "        assertThrows(ChaincodeException.class, () -> contract.CreateProcess(ctx, \"case-003\"));\n"
                + "        verify(stub, never()).putStringState(anyString(), anyString());\n"
                + "    }\n\n"
                + "    @Test\n"
                + "    void rejectsMissingProcessRead() {\n"
                + "        when(stub.getStringState(\"PROC_V2_missing\")).thenReturn(\"\");\n\n"
                + "        assertThrows(ChaincodeException.class, () -> contract.ReadProcess(ctx, \"missing\"));\n"
                + "    }\n"
                + "}\n";
    }

    private String renderTestIdentityHelper() {
        return "    private static ChaincodeStub stubWithCreator() {\n"
                + "        ChaincodeStub stub = mock(ChaincodeStub.class);\n"
                + "        byte[] creator = SerializedIdentity.newBuilder()\n"
                + "                .setMspid(\"Org1MSP\")\n"
                + "                .setIdBytes(ByteString.copyFromUtf8(testCertificate()))\n"
                + "                .build()\n"
                + "                .toByteArray();\n"
                + "        when(stub.getCreator()).thenReturn(creator);\n"
                + "        return stub;\n"
                + "    }\n\n"
                + "    private static String testCertificate() {\n"
                + "        return \"-----BEGIN CERTIFICATE-----\\n\"\n"
                + "                + \"MIIC2DCCAn6gAwIBAgIUTfcXDyxCS+2EQnznfjERUo4Vri8wCgYIKoZIzj0EAwIw\\n\"\n"
                + "                + \"aDELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMRQwEgYDVQQK\\n\"\n"
                + "                + \"EwtIeXBlcmxlZGdlcjEPMA0GA1UECxMGRmFicmljMRkwFwYDVQQDExBmYWJyaWMt\\n\"\n"
                + "                + \"Y2Etc2VydmVyMB4XDTIxMDkyMDExNDEwMFoXDTIyMDkyMDExNDYwMFowYTELMAkG\\n\"\n"
                + "                + \"A1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMRQwEgYDVQQKEwtIeXBl\\n\"\n"
                + "                + \"cmxlZGdlcjEOMAwGA1UECxMFYWRtaW4xEzARBgNVBAMTCm9yZzEtYWRtaW4wWTAT\\n\"\n"
                + "                + \"BgcqhkjOPQIBBggqhkjOPQMBBwNCAAT8zvJEg3FgJ5iUA5GO+n/j48bL83STpz7N\\n\"\n"
                + "                + \"TqejWIZNVTraxE4fjT6traKiswme7gT2NY9Jl0Dj4tbif9l2I9+Oo4IBCzCCAQcw\\n\"\n"
                + "                + \"DgYDVR0PAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFO1zWPynvyER\\n\"\n"
                + "                + \"n9ml6XV5VvC9tIjTMB8GA1UdIwQYMBaAFPbIrI+lh8KayoRpW1YStWMhzJZSMCcG\\n\"\n"
                + "                + \"A1UdEQQgMB6CHG9yZzEtdGxzLWNhLTg1NjdiOTg5OWYtdzU3amYwfgYIKgMEBQYH\\n\"\n"
                + "                + \"CAEEcnsiYXR0cnMiOnsiYWJhYy5pbml0IjoidHJ1ZSIsImFkbWluIjoidHJ1ZSIs\\n\"\n"
                + "                + \"ImhmLkFmZmlsaWF0aW9uIjoiIiwiaGYuRW5yb2xsbWVudElEIjoib3JnMS1hZG1p\\n\"\n"
                + "                + \"biIsImhmLlR5cGUiOiJhZG1pbiJ9fTAKBggqhkjOPQQDAgNIADBFAiEAv99I2J9t\\n\"\n"
                + "                + \"WtOmIzpYix8OFl4Z+ZGRHtay83ux//sZP+MCID02hFqnNpOL/ggGFaDVpVQ/eu0t\\n\"\n"
                + "                + \"KTfVxZEMyZnJtAhp\\n\"\n"
                + "                + \"-----END CERTIFICATE-----\\n\";\n"
                + "    }\n\n";
    }

    private String renderPom(final String artifactId) {
        return "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <groupId>org.example</groupId>\n"
                + "    <artifactId>" + artifactId + "</artifactId>\n"
                + "    <version>1.0.0</version>\n"
                + "    <name>BPMN Generated Chaincode V2</name>\n\n"
                + "    <properties>\n"
                + "        <maven.compiler.source>11</maven.compiler.source>\n"
                + "        <maven.compiler.target>11</maven.compiler.target>\n"
                + "        <fabric.chaincode.java.version>2.5.8</fabric.chaincode.java.version>\n"
                + "        <junit.jupiter.version>5.10.2</junit.jupiter.version>\n"
                + "        <mockito.version>5.11.0</mockito.version>\n"
                + "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n"
                + "    </properties>\n\n"
                + "    <dependencies>\n"
                + "        <dependency>\n"
                + "            <groupId>org.hyperledger.fabric-chaincode-java</groupId>\n"
                + "            <artifactId>fabric-chaincode-shim</artifactId>\n"
                + "            <version>${fabric.chaincode.java.version}</version>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>com.owlike</groupId>\n"
                + "            <artifactId>genson</artifactId>\n"
                + "            <version>1.6</version>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.junit.jupiter</groupId>\n"
                + "            <artifactId>junit-jupiter</artifactId>\n"
                + "            <version>${junit.jupiter.version}</version>\n"
                + "            <scope>test</scope>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.mockito</groupId>\n"
                + "            <artifactId>mockito-core</artifactId>\n"
                + "            <version>${mockito.version}</version>\n"
                + "            <scope>test</scope>\n"
                + "        </dependency>\n"
                + "    </dependencies>\n\n"
                + "    <build>\n"
                + "        <plugins>\n"
                + "            <plugin>\n"
                + "                <groupId>org.apache.maven.plugins</groupId>\n"
                + "                <artifactId>maven-compiler-plugin</artifactId>\n"
                + "                <version>3.12.1</version>\n"
                + "            </plugin>\n"
                + "            <plugin>\n"
                + "                <groupId>org.apache.maven.plugins</groupId>\n"
                + "                <artifactId>maven-surefire-plugin</artifactId>\n"
                + "                <version>3.2.5</version>\n"
                + "            </plugin>\n"
                + "            <plugin>\n"
                + "                <groupId>org.apache.maven.plugins</groupId>\n"
                + "                <artifactId>maven-shade-plugin</artifactId>\n"
                + "                <version>3.5.3</version>\n"
                + "                <executions>\n"
                + "                    <execution>\n"
                + "                        <phase>package</phase>\n"
                + "                        <goals><goal>shade</goal></goals>\n"
                + "                        <configuration>\n"
                + "                            <createDependencyReducedPom>false</createDependencyReducedPom>\n"
                + "                            <filters>\n"
                + "                                <filter>\n"
                + "                                    <artifact>*:*</artifact>\n"
                + "                                    <excludes>\n"
                + "                                        <exclude>META-INF/*.SF</exclude>\n"
                + "                                        <exclude>META-INF/*.DSA</exclude>\n"
                + "                                        <exclude>META-INF/*.RSA</exclude>\n"
                + "                                    </excludes>\n"
                + "                                </filter>\n"
                + "                            </filters>\n"
                + "                            <transformers>\n"
                + "                                <transformer implementation=\"org.apache.maven.plugins.shade.resource.ManifestResourceTransformer\">\n"
                + "                                    <mainClass>org.hyperledger.fabric.contract.ContractRouter</mainClass>\n"
                + "                                </transformer>\n"
                + "                            </transformers>\n"
                + "                        </configuration>\n"
                + "                    </execution>\n"
                + "                </executions>\n"
                + "            </plugin>\n"
                + "        </plugins>\n"
                + "    </build>\n"
                + "</project>\n";
    }

    private String renderGradleBuild(final String chaincodeName) {
        return """
                plugins {
                    id 'application'
                    id 'java'
                }

                group = 'org.example'
                version = '1.0.0'

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'org.hyperledger.fabric-chaincode-java:fabric-chaincode-shim:2.5.8'
                    implementation 'com.owlike:genson:1.6'

                    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
                    testImplementation 'org.mockito:mockito-core:5.11.0'
                    testImplementation 'org.hyperledger.fabric:fabric-protos:0.3.7'
                    testImplementation 'com.google.protobuf:protobuf-java:4.33.4'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
                }

                tasks.withType(JavaCompile).configureEach {
                    options.release = 11
                    options.encoding = 'UTF-8'
                }

                application {
                    applicationName = '%s'
                    mainClass = 'org.hyperledger.fabric.contract.ContractRouter'
                }

                test {
                    useJUnitPlatform()
                }
                """.formatted(chaincodeName);
    }

    private String renderGradleSettings(final String artifactId) {
        return "rootProject.name = '" + artifactId + "'\n";
    }

    private void copyGradleWrapper(final Path outputProjectDir) throws IOException {
        final Path wrapperDir = outputProjectDir.resolve("gradle/wrapper");
        Files.createDirectories(wrapperDir);

        copyResource("/gradle-wrapper/gradlew", outputProjectDir.resolve("gradlew"));
        copyResource("/gradle-wrapper/gradlew.bat", outputProjectDir.resolve("gradlew.bat"));
        copyResource("/gradle-wrapper/gradle-wrapper.jar", wrapperDir.resolve("gradle-wrapper.jar"));
        copyResource("/gradle-wrapper/gradle-wrapper.properties", wrapperDir.resolve("gradle-wrapper.properties"));

        final Path gradlew = outputProjectDir.resolve("gradlew");
        if (!gradlew.toFile().setExecutable(true, false) && !Files.isExecutable(gradlew)) {
            throw new IOException("Could not make generated Gradle wrapper executable: " + gradlew);
        }
    }

    private void copyResource(final String resourceName, final Path destination) throws IOException {
        try (InputStream input = FabricContractProjectGeneratorV2.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing generator resource: " + resourceName);
            }
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String renderReadme(
            final String packageName,
            final String contractClass,
            final BpmnProcessModel model,
            final String artifactId) {
        return "# Generated Chaincode V2\n\n"
                + "Generated from BPMN process: `" + model.getProcessName() + "` (`" + model.getProcessId() + "`)\n\n"
                + "## Build\n\n"
                + "```bash\n"
                + "./gradlew clean installDist\n"
                + "```\n\n"
                + "The Fabric-compatible application distribution is generated under `build/install`.\n\n"
                + "## Test\n\n"
                + "```bash\n"
                + "./gradlew test\n"
                + "```\n\n"
                + "The Maven build remains available through `mvn clean package` and `mvn test`.\n\n"
                + "Generated baseline tests cover process creation, persisted state reads, and validation failures.\n\n"
                + "## Contract\n\n"
                + "- Contract class: `" + packageName + "." + contractClass + "`\n"
                + "- Artifact id: `" + artifactId + "`\n\n"
                + "The generated contract includes one submit transaction per BPMN task plus generic `CompleteActivity`.\n"
                + "Parallel joins are synchronized and exclusive gateways route by metadata values.\n";
    }

    private static String typeToken(final BpmnNodeType type) {
        return switch (type) {
            case START_EVENT -> "startEvent";
            case END_EVENT -> "endEvent";
            case TASK -> "task";
            case EXCLUSIVE_GATEWAY -> "exclusiveGateway";
            case PARALLEL_GATEWAY -> "parallelGateway";
        };
    }

    private static String uniqueMethodName(final String base, final Set<String> used) {
        if (!used.contains(base)) {
            used.add(base);
            return base;
        }

        int i = 2;
        while (used.contains(base + i)) {
            i++;
        }
        String candidate = base + i;
        used.add(candidate);
        return candidate;
    }

    private static void line(final StringBuilder sb, final String text) {
        sb.append(text).append('\n');
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

    private static class ConditionDescriptor {
        private final String variableName;
        private final boolean expectedValue;

        private ConditionDescriptor(final String variableName, final boolean expectedValue) {
            this.variableName = variableName;
            this.expectedValue = expectedValue;
        }
    }

    public static class GeneratedContractInfo {
        private final Path projectDir;
        private final String packageName;
        private final String contractClassName;

        public GeneratedContractInfo(final Path projectDir, final String packageName, final String contractClassName) {
            this.projectDir = projectDir;
            this.packageName = packageName;
            this.contractClassName = contractClassName;
        }

        public Path getProjectDir() {
            return projectDir;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getContractClassName() {
            return contractClassName;
        }
    }
}
