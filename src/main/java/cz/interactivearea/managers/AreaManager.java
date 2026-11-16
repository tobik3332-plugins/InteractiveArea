package cz.interactivearea.managers;

import cz.interactivearea.InteractiveAreaPlugin;
import cz.interactivearea.models.Area;
import cz.interactivearea.models.AreaAction;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AreaManager {

    private final InteractiveAreaPlugin plugin;
    private final File areasFolder;
    private final Map<String, Area> areas = new HashMap<>();

    // Docasne uloziste pro proces vytvareni
    private final Set<String> creationSessions = new HashSet<>();
    private final Map<String, Location> tempPos1 = new HashMap<>();
    private final Map<String, Location> tempPos2 = new HashMap<>();

    // Docasne uloziste pro potvrzeni smazani (UUID hrace -> nazev oblasti + cas)
    private final Map<UUID, DeleteConfirmation> deleteConfirmations = new HashMap<>();

    // Docasne uloziste pro chat editaci akci
    private final Map<UUID, PendingEdit> pendingEdits = new HashMap<>();

    public record DeleteConfirmation(String areaName, long timestamp) {}
    public record PendingEdit(String areaName, String actionId) {}

    public AreaManager(InteractiveAreaPlugin plugin) {
        this.plugin = plugin;
        this.areasFolder = new File(plugin.getDataFolder(), "areas");
        if (!areasFolder.exists()) {
            areasFolder.mkdirs();
        }
    }

    public void loadAreas() {
        areas.clear();
        File[] files = areasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = file.getName().replace(".yml", "");
            String world = config.getString("world", "world");
            double minX = config.getDouble("bounds.minX");
            double minY = config.getDouble("bounds.minY");
            double minZ = config.getDouble("bounds.minZ");
            double maxX = config.getDouble("bounds.maxX");
            double maxY = config.getDouble("bounds.maxY");
            double maxZ = config.getDouble("bounds.maxZ");
            String permission = config.getString("permission", "");

            List<AreaAction> actions = new ArrayList<>();

            // Nacteni teleportu
            if (config.isConfigurationSection("actions.teleports")) {
                ConfigurationSection sec = config.getConfigurationSection("actions.teleports");
                for (String key : sec.getKeys(false)) {
                    actions.add(new AreaAction(key, AreaAction.ActionType.TELEPORT, null, sec.getString(key)));
                }
            }

            // Nacteni prikazu - player
            if (config.isConfigurationSection("actions.commands.player")) {
                ConfigurationSection sec = config.getConfigurationSection("actions.commands.player");
                for (String key : sec.getKeys(false)) {
                    actions.add(new AreaAction(key, AreaAction.ActionType.COMMAND, AreaAction.CommandExecutorType.PLAYER, sec.getString(key)));
                }
            }

            // Nacteni prikazu - console
            if (config.isConfigurationSection("actions.commands.console")) {
                ConfigurationSection sec = config.getConfigurationSection("actions.commands.console");
                for (String key : sec.getKeys(false)) {
                    actions.add(new AreaAction(key, AreaAction.ActionType.COMMAND, AreaAction.CommandExecutorType.CONSOLE, sec.getString(key)));
                }
            }

            // Nacteni textu - message
            if (config.isConfigurationSection("actions.texts.message")) {
                ConfigurationSection sec = config.getConfigurationSection("actions.texts.message");
                for (String key : sec.getKeys(false)) {
                    actions.add(new AreaAction(key, AreaAction.ActionType.TEXT_MESSAGE, null, sec.getString(key)));
                }
            }

            // Nacteni textu - broadcast
            if (config.isConfigurationSection("actions.texts.broadcast")) {
                ConfigurationSection sec = config.getConfigurationSection("actions.texts.broadcast");
                for (String key : sec.getKeys(false)) {
                    actions.add(new AreaAction(key, AreaAction.ActionType.TEXT_BROADCAST, null, sec.getString(key)));
                }
            }

            areas.put(name.toLowerCase(), new Area(name, world, minX, minY, minZ, maxX, maxY, maxZ, permission, actions));
        }
    }

    public void saveArea(Area area) {
        File file = new File(areasFolder, area.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("name", area.getName());
        config.set("world", area.getWorldName());
        config.set("permission", area.getPermission());

        config.set("bounds.minX", area.getMinX());
        config.set("bounds.minY", area.getMinY());
        config.set("bounds.minZ", area.getMinZ());
        config.set("bounds.maxX", area.getMaxX());
        config.set("bounds.maxY", area.getMaxY());
        config.set("bounds.maxZ", area.getMaxZ());

        // Vytvoreni struktury sekci ihned po vytvoreni souboru
        config.createSection("actions.teleports");
        config.createSection("actions.commands.player");
        config.createSection("actions.commands.console");
        config.createSection("actions.texts.message");
        config.createSection("actions.texts.broadcast");

        for (AreaAction action : area.getActions()) {
            switch (action.getType()) {
                case TELEPORT -> config.set("actions.teleports." + action.getId(), action.getValue());
                case COMMAND -> {
                    if (action.getExecutorType() == AreaAction.CommandExecutorType.PLAYER) {
                        config.set("actions.commands.player." + action.getId(), action.getValue());
                    } else {
                        config.set("actions.commands.console." + action.getId(), action.getValue());
                    }
                }
                case TEXT_MESSAGE -> config.set("actions.texts.message." + action.getId(), action.getValue());
                case TEXT_BROADCAST -> config.set("actions.texts.broadcast." + action.getId(), action.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nepodarilo se ulozit oblast " + area.getName() + ": " + e.getMessage());
        }
    }

    public void deleteArea(String name) {
        Area removed = areas.remove(name.toLowerCase());
        if (removed != null) {
            File file = new File(areasFolder, removed.getName() + ".yml");
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public Area getArea(String name) {
        return areas.get(name.toLowerCase());
    }

    public Collection<Area> getAllAreas() {
        return areas.values();
    }

    public void registerArea(Area area) {
        areas.put(area.getName().toLowerCase(), area);
        saveArea(area);
    }

    // Metody pro proces vytvareni
    public boolean isCreating(String name) { return creationSessions.contains(name.toLowerCase()); }
    public void startCreating(String name) { creationSessions.add(name.toLowerCase()); }
    public void finishCreating(String name) {
        String key = name.toLowerCase();
        creationSessions.remove(key);
        tempPos1.remove(key);
        tempPos2.remove(key);
    }
    public void setPos1(String name, Location loc) { tempPos1.put(name.toLowerCase(), loc); }
    public void setPos2(String name, Location loc) { tempPos2.put(name.toLowerCase(), loc); }
    public Location getPos1(String name) { return tempPos1.get(name.toLowerCase()); }
    public Location getPos2(String name) { return tempPos2.get(name.toLowerCase()); }

    // Potvrzeni smazani
    public Map<UUID, DeleteConfirmation> getDeleteConfirmations() { return deleteConfirmations; }

    // Chat editace
    public Map<UUID, PendingEdit> getPendingEdits() { return pendingEdits; }
}
