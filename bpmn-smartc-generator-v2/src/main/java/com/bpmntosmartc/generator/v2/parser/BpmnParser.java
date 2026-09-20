package com.bpmntosmartc.generator.v2.parser;

import com.bpmntosmartc.generator.v2.model.BpmnFlow;
import com.bpmntosmartc.generator.v2.model.BpmnNode;
import com.bpmntosmartc.generator.v2.model.BpmnNodeType;
import com.bpmntosmartc.generator.v2.model.BpmnProcessModel;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class BpmnParser {

    public BpmnProcessModel parse(final Path bpmnFile) throws Exception {
        final String xml = Files.readString(bpmnFile);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xml)));

        Element processElement = findFirstByLocalName(doc, "process");
        if (processElement == null) {
            throw new IllegalArgumentException("No BPMN process element found in file: " + bpmnFile);
        }

        String processId = processElement.getAttribute("id");
        String processName = processElement.getAttribute("name");
        if (processName == null || processName.isBlank()) {
            processName = processId;
        }

        List<BpmnNode> nodes = new ArrayList<>();
        List<BpmnFlow> flows = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();

        NodeList children = processElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) child;
            String localName = localName(element);

            if ("sequenceFlow".equals(localName)) {
                flows.add(new BpmnFlow(
                        element.getAttribute("id"),
                        element.getAttribute("sourceRef"),
                        element.getAttribute("targetRef"),
                        element.getAttribute("name"),
                        findConditionExpression(element)));
                continue;
            }

            BpmnNodeType nodeType = BpmnNodeType.fromLocalName(localName);
            if (nodeType != null) {
                String defaultFlow = nodeType == BpmnNodeType.EXCLUSIVE_GATEWAY
                        ? nullIfBlank(element.getAttribute("default"))
                        : null;
                nodes.add(new BpmnNode(
                        element.getAttribute("id"),
                        element.getAttribute("name"),
                        nodeType,
                        defaultFlow));
                continue;
            }

            unsupported.add(localName);
        }

        return new BpmnProcessModel(processId, processName, nodes, flows, unsupported);
    }

    private static Element findFirstByLocalName(final Document doc, final String wanted) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            if (wanted.equals(localName(element))) {
                return element;
            }
        }
        return null;
    }

    private static String findConditionExpression(final Element flowElement) {
        NodeList children = flowElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) child;
            if ("conditionExpression".equals(localName(element))) {
                String value = element.getTextContent();
                return value == null ? "" : value.trim();
            }
        }
        return "";
    }

    private static String localName(final Element element) {
        String local = element.getLocalName();
        if (local != null && !local.isBlank()) {
            return local;
        }

        String tag = element.getTagName();
        int idx = tag.lastIndexOf(':');
        if (idx >= 0 && idx < tag.length() - 1) {
            return tag.substring(idx + 1);
        }
        return tag;
    }

    private static String nullIfBlank(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
