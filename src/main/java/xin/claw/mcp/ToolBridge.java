package xin.claw.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class ToolBridge {

    private static final Logger logger = LoggerFactory.getLogger(ToolBridge.class);

    private final List<ToolEntry> tools = new ArrayList<>();

    public ToolBridge() {
        registerTools();
    }

    private void registerTools() {
        String[] toolClasses = {
            "xin.claw.tools.MovementTools",
            "xin.claw.tools.PerceptionTools",
            "xin.claw.tools.ActionTools",
            "xin.claw.tools.SocialTools",
            "xin.claw.tools.InventoryTools",
            "xin.claw.tools.SystemTools"
        };

        for (String className : toolClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();
                register(instance);
                logger.info("[MovementMCP] Loaded tool class: {}", className);
            } catch (ClassNotFoundException e) {
                logger.warn("[MovementMCP] Tool class not found (is XinClaw loaded?): {}", className);
            } catch (Exception e) {
                logger.error("[MovementMCP] Failed to load tool class: {}", className, e);
            }
        }
        logger.info("[MovementMCP] Registered {} tools", tools.size());
    }

    @SuppressWarnings("unchecked")
    private void register(Object toolInstance) {
        Class<? extends Annotation> toolClass = (Class<? extends Annotation>) findToolAnnotation();
        Class<? extends Annotation> pClass = (Class<? extends Annotation>) findPAnnotation();
        if (toolClass == null) return;

        for (Method method : toolInstance.getClass().getDeclaredMethods()) {
            Annotation toolAnnotation = method.getAnnotation(toolClass);
            if (toolAnnotation == null) continue;

            method.setAccessible(true);

            String name = method.getName();
            String description = getToolDescription(toolAnnotation);

            JsonObject propsObj = new JsonObject();
            JsonArray required = new JsonArray();

            Parameter[] params = method.getParameters();
            Annotation[] pAnnotations = pClass != null ? method.getAnnotationsByType(pClass) : new Annotation[0];

            for (int i = 0; i < params.length; i++) {
                String paramName = params[i].getName();
                String paramDesc = "";
                if (i < pAnnotations.length) {
                    paramDesc = getPDescription(pAnnotations[i]);
                }

                JsonObject paramSchema = new JsonObject();
                Class<?> type = params[i].getType();
                if (type == int.class || type == Integer.class) {
                    paramSchema.addProperty("type", "integer");
                } else if (type == double.class || type == Double.class ||
                           type == float.class || type == Float.class ||
                           type == long.class || type == Long.class) {
                    paramSchema.addProperty("type", "number");
                } else if (type == boolean.class || type == Boolean.class) {
                    paramSchema.addProperty("type", "boolean");
                } else {
                    paramSchema.addProperty("type", "string");
                }
                paramSchema.addProperty("description", paramDesc);
                propsObj.add(paramName, paramSchema);
                required.add(paramName);
            }

            JsonObject inputSchema = new JsonObject();
            inputSchema.addProperty("type", "object");
            inputSchema.add("properties", propsObj);
            inputSchema.add("required", required);

            tools.add(new ToolEntry(name, description, inputSchema, toolInstance, method));
        }
    }

    private Class<?> findToolAnnotation() {
        try {
            return Class.forName("dev.langchain4j.agent.tool.Tool");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Class<?> findPAnnotation() {
        try {
            return Class.forName("dev.langchain4j.agent.tool.P");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private String getToolDescription(Annotation toolAnnotation) {
        try {
            Method valueMethod = toolAnnotation.getClass().getMethod("value");
            return (String) valueMethod.invoke(toolAnnotation);
        } catch (Exception e) {
            return "";
        }
    }

    private String getPDescription(Annotation pAnnotation) {
        try {
            Method valueMethod = pAnnotation.getClass().getMethod("value");
            return (String) valueMethod.invoke(pAnnotation);
        } catch (Exception e) {
            return "";
        }
    }

    public JsonArray listTools() {
        JsonArray array = new JsonArray();
        for (ToolEntry entry : tools) {
            JsonObject tool = new JsonObject();
            tool.addProperty("name", entry.name);
            tool.addProperty("description", entry.description);
            tool.add("inputSchema", entry.inputSchema);
            array.add(tool);
        }
        return array;
    }

    public String callTool(String name, JsonObject arguments) throws Exception {
        ToolEntry target = null;
        for (ToolEntry entry : tools) {
            if (entry.name.equals(name)) {
                target = entry;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }

        Parameter[] params = target.method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            String paramName = params[i].getName();
            Class<?> type = params[i].getType();

            if (!arguments.has(paramName)) {
                if (type == int.class) args[i] = 0;
                else if (type == double.class) args[i] = 0.0;
                else if (type == long.class) args[i] = 0L;
                else if (type == float.class) args[i] = 0.0f;
                else if (type == boolean.class) args[i] = false;
                else args[i] = null;
                continue;
            }

            com.google.gson.JsonElement argElement = arguments.get(paramName);

            if (type == int.class || type == Integer.class) {
                args[i] = argElement.getAsInt();
            } else if (type == double.class || type == Double.class) {
                args[i] = argElement.getAsDouble();
            } else if (type == long.class || type == Long.class) {
                args[i] = argElement.getAsLong();
            } else if (type == float.class || type == Float.class) {
                args[i] = argElement.getAsFloat();
            } else if (type == boolean.class || type == Boolean.class) {
                args[i] = argElement.getAsBoolean();
            } else {
                args[i] = argElement.getAsString();
            }
        }

        logger.info("[MovementMCP] Calling tool: {}({})", name, Arrays.toString(args));
        Object result = target.method.invoke(target.instance, args);
        return result != null ? result.toString() : "OK";
    }

    private static class ToolEntry {
        final String name;
        final String description;
        final JsonObject inputSchema;
        final Object instance;
        final Method method;

        ToolEntry(String name, String description, JsonObject inputSchema, Object instance, Method method) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
            this.instance = instance;
            this.method = method;
        }
    }
}
