package movementmcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class ToolBridge {

    private static final Logger logger = LoggerFactory.getLogger(ToolBridge.class);

    private final List<BaseMcpTools.ToolDef> toolDefs = new ArrayList<>();
    private final Map<String, Function<JsonObject, String>> handlers = new HashMap<>();

    public ToolBridge() {
        loadAll(new MovementTools(), new PerceptionTools(), new ActionTools(), new SocialTools(), new SystemTools());
        logger.info("[MovementMCP] Registered {} tools", toolDefs.size());
    }

    private void loadAll(BaseMcpTools... tools) {
        for (BaseMcpTools t : tools) {
            t.init();
            toolDefs.addAll(t.defs);
            handlers.putAll(t.handlers);
        }
    }

    public JsonArray listTools() {
        JsonArray arr = new JsonArray();
        for (var td : toolDefs) {
            JsonObject t = new JsonObject();
            t.addProperty("name", td.name());
            t.addProperty("description", td.desc());
            t.add("inputSchema", td.schema());
            arr.add(t);
        }
        return arr;
    }

    public String callTool(String name, JsonObject args) throws Exception {
        Function<JsonObject, String> h = handlers.get(name);
        if (h == null) throw new IllegalArgumentException("Unknown tool: " + name);
        return h.apply(args);
    }
}
