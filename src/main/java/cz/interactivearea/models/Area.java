package cz.interactivearea.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class Area {

    private final String name;
    private String worldName;
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private String permission;
    private final List<AreaAction> actions;

    public Area(String name, Location pos1, Location pos2) {
        this.name = name;
        this.worldName = pos1.getWorld().getName();
        setBounds(pos1, pos2);
        this.permission = "";
        this.actions = new ArrayList<>();
    }

    public Area(String name, String worldName, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, String permission, List<AreaAction> actions) {
        this.name = name;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.permission = permission != null ? permission : "";
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public void setBounds(Location pos1, Location pos2) {
        this.worldName = pos1.getWorld().getName();
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX()) + 1.0;
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY()) + 1.0;
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ()) + 1.0;
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    public Location getCenterLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, (minX + maxX) / 2.0, minY + 0.5, (minZ + maxZ) / 2.0);
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public List<AreaAction> getActions() { return actions; }

    public AreaAction getActionById(String id) {
        for (AreaAction action : actions) {
            if (action.getId().equals(id)) return action;
        }
        return null;
    }
}
