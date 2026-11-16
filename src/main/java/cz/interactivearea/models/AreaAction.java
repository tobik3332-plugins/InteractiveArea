package cz.interactivearea.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class AreaAction {

    public enum ActionType {
        TELEPORT("teleport"),
        COMMAND("command"),
        TEXT_MESSAGE("text-message"),
        TEXT_BROADCAST("text-brodcast");

        private final String displayName;

        ActionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum CommandExecutorType {
        PLAYER, CONSOLE
    }

    private final String id;
    private final ActionType type;
    private CommandExecutorType executorType; // Pouze pro COMMAND
    private String value; // Raw hodnota (pro text s barvami, pro prikaz bez /, pro tp "world x y z")

    public AreaAction(ActionType type, String value) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.value = value;
    }

    public AreaAction(String id, ActionType type, CommandExecutorType executorType, String value) {
        this.id = id;
        this.type = type;
        this.executorType = executorType;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public ActionType getType() {
        return type;
    }

    public CommandExecutorType getExecutorType() {
        return executorType;
    }

    public void setExecutorType(CommandExecutorType executorType) {
        this.executorType = executorType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Location parseTeleportLocation() {
        if (type != ActionType.TELEPORT || value == null) return null;
        String[] parts = value.split(" ");
        if (parts.length < 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length >= 5 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length >= 6 ? Float.parseFloat(parts[5]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getCleanTeleportSuggest() {
        if (type != ActionType.TELEPORT || value == null) return "";
        String[] parts = value.split(" ");
        if (parts.length >= 4) {
            return parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
        }
        return value;
    }
}
