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

@Component
public class ConfigLoader {
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode rootConfig;
    private JsonNode userConfig;

    public void loadRootConfig(File rootConfigFile) throws IOException {
        rootConfig = mapper.readTree(rootConfigFile);
    }

    public void loadUserConfig(File userConfigFile) throws IOException {
        userConfig = mapper.readTree(userConfigFile);
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

    public Map<String, JsonNode> mergeConfigs() {
        JsonNode settingsNode = userConfig.get("settings");
        JsonNode workersNode = userConfig.get("workers");

        JsonNode defaultConfig;
        if (rootConfig != null) {
            defaultConfig = deepMerge(rootConfig.deepCopy(), settingsNode);
        } else {
            defaultConfig = settingsNode;
        }

        Map<String, JsonNode> workerConfigs = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> workers = workersNode.fields();
        while (workers.hasNext()) {
            Map.Entry<String, JsonNode> worker = workers.next();

            JsonNode workerConfig = defaultConfig.deepCopy();
            ((ObjectNode) workerConfig).put("_name", worker.getKey());

            deepMerge(defaultConfig, worker.getValue());
            workerConfigs.put(worker.getKey(), workerConfig);
        }

        return workerConfigs;
    }
}
