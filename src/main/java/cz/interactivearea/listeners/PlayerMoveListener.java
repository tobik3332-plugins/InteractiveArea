package cz.interactivearea.listeners;

import cz.interactivearea.managers.AreaManager;
import cz.interactivearea.models.Area;
import cz.interactivearea.models.AreaAction;
import cz.interactivearea.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class PlayerMoveListener implements Listener {

    private final AreaManager areaManager;
    private final Map<UUID, Set<String>> playersInAreas = new HashMap<>();

    public PlayerMoveListener(AreaManager areaManager) {
        this.areaManager = areaManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (event.getFrom().getX() == to.getX()
                && event.getFrom().getY() == to.getY()
                && event.getFrom().getZ() == to.getZ()) {
            return;
        }

        Player player = event.getPlayer();
        Set<String> insideNow = playersInAreas.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

        for (Area area : areaManager.getAllAreas()) {
            String key = area.getName().toLowerCase();
            boolean isInside = area.contains(to);

            if (isInside && !insideNow.contains(key)) {
                insideNow.add(key);
                executeAreaActions(player, area);
            } else if (!isInside && insideNow.contains(key)) {
                insideNow.remove(key);
            }
        }
    }

    private void executeAreaActions(Player player, Area area) {
        String perm = area.getPermission();
        if (perm != null && !perm.isEmpty() && !player.hasPermission(perm)) {
            return;
        }

        for (AreaAction action : area.getActions()) {
            switch (action.getType()) {
                case TELEPORT -> {
                    Location target = action.parseTeleportLocation();
                    if (target != null) {
                        player.teleport(target);
                    }
                }
                case COMMAND_PLAYER -> {
                    String cmd = action.getValue().replace("%player%", player.getName());
                    player.performCommand(cmd);
                }
                case COMMAND_CONSOLE -> {
                    String cmd = action.getValue().replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                case TEXT_MESSAGE -> {
                    String msg = action.getValue().replace("%player%", player.getName());
                    player.sendMessage(MessageUtil.parse(msg));
                }
                case TEXT_BROADCAST -> {
                    String msg = action.getValue().replace("%player%", player.getName());
                    Component parsed = MessageUtil.parse(msg);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(parsed);
                    }
                }
            }
        }
    }
}
