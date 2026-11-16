package cz.interactivearea.managers;

import cz.interactivearea.InteractiveAreaPlugin;
import cz.interactivearea.models.Area;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class VisualizerManager {

    private final InteractiveAreaPlugin plugin;
    private final AreaManager areaManager;
    private final Set<String> visualizedAreas = new HashSet<>();
    private BukkitTask task;

    public VisualizerManager(InteractiveAreaPlugin plugin, AreaManager areaManager) {
        this.plugin = plugin;
        this.areaManager = areaManager;
    }

    public void setVisualizing(String areaName, boolean state) {
        if (state) {
            visualizedAreas.add(areaName.toLowerCase());
        } else {
            visualizedAreas.remove(areaName.toLowerCase());
        }
    }

    public void startTask() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (visualizedAreas.isEmpty()) return;

            String particleName = plugin.getConfig().getString("settings.visualize-particle", "CLOUD").toUpperCase();
            Particle particle;
            try {
                particle = Particle.valueOf(particleName);
            } catch (IllegalArgumentException e) {
                particle = Particle.CLOUD;
            }

            for (String areaKey : new HashSet<>(visualizedAreas)) {
                Area area = areaManager.getArea(areaKey);
                if (area == null) {
                    visualizedAreas.remove(areaKey);
                    continue;
                }
                drawBox(area, particle);
            }
        }, 10L, 10L);
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
        }
    }

    private void drawBox(Area area, Particle particle) {
        World world = Bukkit.getWorld(area.getWorldName());
        if (world == null) return;

        double minX = area.getMinX();
        double minY = area.getMinY();
        double minZ = area.getMinZ();
        double maxX = area.getMaxX();
        double maxY = area.getMaxY();
        double maxZ = area.getMaxZ();

        double step = 0.5;

        // Hrany podel osy X
        for (double x = minX; x <= maxX; x += step) {
            spawn(world, particle, x, minY, minZ);
            spawn(world, particle, x, maxY, minZ);
            spawn(world, particle, x, minY, maxZ);
            spawn(world, particle, x, maxY, maxZ);
        }

        // Hrany podel osy Y
        for (double y = minY; y <= maxY; y += step) {
            spawn(world, particle, minX, y, minZ);
            spawn(world, particle, maxX, y, minZ);
            spawn(world, particle, minX, y, maxZ);
            spawn(world, particle, maxX, y, maxZ);
        }

        // Hrany podel osy Z
        for (double z = minZ; z <= maxZ; z += step) {
            spawn(world, particle, minX, minY, z);
            spawn(world, particle, maxX, minY, z);
            spawn(world, particle, minX, maxY, z);
            spawn(world, particle, maxX, maxY, z);
        }
    }

    private void spawn(World world, Particle particle, double x, double y, double z) {
        world.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
    }
}
