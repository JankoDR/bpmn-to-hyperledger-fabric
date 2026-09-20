package com.bpmntosmartc.generator.v2.util;

public final class NameUtils {
    private NameUtils() {
    }

    public static String toPackageToken(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "generated";
        }

        String normalized = raw.replaceAll("[^A-Za-z0-9]+", "_").toLowerCase();
        normalized = normalized.replaceAll("_+", "_");
        if (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            normalized = "generated";
        }
        if (!Character.isLetter(normalized.charAt(0))) {
            normalized = "p_" + normalized;
        }
        return normalized;
    }

    public static String toClassName(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "GeneratedProcess";
        }

        String[] parts = raw.replaceAll("[^A-Za-z0-9]+", " ").trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String clean = part.replaceAll("[^A-Za-z0-9]", "");
            if (clean.isBlank()) {
                continue;
            }
            sb.append(Character.toUpperCase(clean.charAt(0)));
            if (clean.length() > 1) {
                sb.append(clean.substring(1));
            }
        }

        if (sb.length() == 0) {
            sb.append("GeneratedProcess");
        }

        if (!Character.isLetter(sb.charAt(0))) {
            sb.insert(0, 'P');
        }

        return sb.toString();
    }

    public static String toMethodToken(final String raw) {
        String className = toClassName(raw);
        if (className.isBlank()) {
            return "activity";
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    public static String javaStringLiteral(final String text) {
        if (text == null) {
            return "\"\"";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < 32 || ch > 126) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
