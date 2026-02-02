package com.restfulbooker.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CucumberData {

    private static final String DATA_FILE_PATH = "C:\\Users\\Prakhar.singh\\IdeaProjects\\restful-booker\\src\\test\\resources\\data\\testdata.json";
    private static JsonNode rootNode;

    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            rootNode = mapper.readTree(new File(DATA_FILE_PATH));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load testdata.json from " + DATA_FILE_PATH, e);
        }
    }

    private static final ThreadLocal<Map<String, Object>> SAVED_VALUES = ThreadLocal.withInitial(HashMap::new);

    public static Object get(String key) {
        if (SAVED_VALUES.get().containsKey(key)) {
            return SAVED_VALUES.get().get(key);
        }

        if (key.contains(":")) {
            String[] parts = key.split(":");
            JsonNode node = rootNode;
            for (String part : parts) {
                if (node.has(part)) {
                    node = node.get(part);
                } else {
                    return key;
                }
            }
            return getValueFromNode(node);
        }

        return key;
    }

    private static Object getValueFromNode(JsonNode node) {
        if (node.isBoolean()) return node.asBoolean();
        if (node.isInt()) return node.asInt();
        if (node.isDouble()) return node.asDouble();
        return node.asText();
    }

    public static void set(String key, Object value) {
        SAVED_VALUES.get().put(key, value);
    }
}