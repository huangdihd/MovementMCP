package movementmcp.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ItemNameResolver {

    private static final Map<Integer, String> itemNames = new HashMap<>();
    private static boolean loaded = false;

    public static void init() {
        if (loaded) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(ItemNameResolver.class.getClassLoader().getResourceAsStream("items.json")),
                StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            List<ItemEntry> entries = new Gson().fromJson(content, new TypeToken<List<ItemEntry>>(){}.getType());
            for (ItemEntry entry : entries) {
                itemNames.put(entry.id, entry.displayName != null ? entry.displayName : entry.name);
            }
            loaded = true;
        } catch (Exception e) {
            // silent
        }
    }

    public static String getItemName(int id) {
        if (!loaded) init();
        String name = itemNames.get(id);
        return name != null ? name : "item_" + id;
    }

    private static class ItemEntry {
        int id;
        String name;
        String displayName;
    }
}
