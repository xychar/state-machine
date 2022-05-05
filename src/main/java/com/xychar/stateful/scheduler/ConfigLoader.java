package com.xychar.stateful.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Three-layer execution configurations.
 */
@Component
public class ConfigLoader {
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode rootConfig = mapper.createObjectNode();
    private JsonNode userConfig = mapper.createObjectNode();

    private Map<String, JsonNode> mergedConfigs = null;

    public void loadRootConfig(File rootConfigFile) throws IOException {
        rootConfig = mapper.readTree(rootConfigFile);
        mergedConfigs = null;
    }

    public void loadUserConfig(File userConfigFile) throws IOException {
        userConfig = mapper.readTree(userConfigFile);
        mergedConfigs = null;
    }

    public static JsonNode deepMerge(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode instanceof ObjectNode) {
            ObjectNode targetNode = (ObjectNode) mainNode;
            Iterator<Map.Entry<String, JsonNode>> fields = updateNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode destNode = mainNode.get(fieldName);
                if (destNode != null && destNode.isObject()) {
                    deepMerge(destNode, field.getValue());
                } else {
                    targetNode.set(fieldName, field.getValue());
                }
            }
        }

        return mainNode;
    }

    public static Map<String, JsonNode> mergeConfigs(JsonNode root, JsonNode user) {
        JsonNode settingsNode = user.get("settings");
        JsonNode workersNode = user.get("workers");

        JsonNode defaultConfig;
        if (root != null) {
            defaultConfig = deepMerge(root.deepCopy(), settingsNode);
        } else {
            defaultConfig = settingsNode;
        }

        Map<String, JsonNode> workerConfigs = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> workers = workersNode.fields();
        while (workers.hasNext()) {
            Map.Entry<String, JsonNode> worker = workers.next();

            JsonNode workerConfig = defaultConfig.deepCopy();
            ((ObjectNode) workerConfig).put("$name", worker.getKey());

            deepMerge(defaultConfig, worker.getValue());
            workerConfigs.put(worker.getKey(), workerConfig);
        }

        return workerConfigs;
    }

    public Map<String, JsonNode> getMergedConfigs() {
        if (mergedConfigs == null) {
            mergedConfigs = mergeConfigs(rootConfig, userConfig);
        }

        return mergedConfigs;
    }
}
