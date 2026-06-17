package movementmcp;

import com.google.gson.JsonObject;
import java.util.List;

public interface McpTool {
    String name();
    String description();
    List<Param> params();
    String execute(JsonObject args);

    record Param(String name, String type, String desc) {
        public Param(String name, String type) { this(name, type, ""); }
        public static Param num(String name) { return new Param(name, "number"); }
        public static Param num(String name, String desc) { return new Param(name, "number", desc); }
        public static Param integer(String name) { return new Param(name, "integer"); }
        public static Param integer(String name, String desc) { return new Param(name, "integer", desc); }
        public static Param str(String name) { return new Param(name, "string"); }
        public static Param str(String name, String desc) { return new Param(name, "string", desc); }
    }
}
