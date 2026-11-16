package cz.interactivearea.listeners;

import cz.interactivearea.InteractiveAreaPlugin;
import cz.interactivearea.commands.IAreaCommand;
import cz.interactivearea.managers.AreaManager;
import cz.interactivearea.models.Area;
import cz.interactivearea.models.AreaAction;
import cz.interactivearea.utils.MessageUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ChatEditListener implements Listener {

    private final InteractiveAreaPlugin plugin;
    private final AreaManager areaManager;

    public ChatEditListener(InteractiveAreaPlugin plugin, AreaManager areaManager) {
        this.plugin = plugin;
        this.areaManager = areaManager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!areaManager.getPendingEdits().containsKey(uuid)) return;

        event.setCancelled(true);
        AreaManager.PendingEdit edit = areaManager.getPendingEdits().remove(uuid);
        String newValue = PlainTextComponentSerializer.plainText().serialize(event.message());

        Bukkit.getScheduler().runTask(plugin, () -> {
            Area area = areaManager.getArea(edit.areaName());
            if (area == null) return;

            AreaAction action = area.getActionById(edit.actionId());
            if (action == null) return;

            if (action.getType() == AreaAction.ActionType.TELEPORT) {
                String[] parts = newValue.trim().split("\\s+");
                if (parts.length != 4) {
                    MessageUtil.sendConfigMessage(player, "invalid-teleport-format");
                    return;
                }
                World world = Bukkit.getWorld(parts[0]);
                if (world == null) {
                    MessageUtil.sendConfigMessage(player, "invalid-teleport-format");
                    return;
                }
                try {
                    Double.parseDouble(parts[1]);
                    Double.parseDouble(parts[2]);
                    Double.parseDouble(parts[3]);
                } catch (NumberFormatException e) {
                    MessageUtil.sendConfigMessage(player, "invalid-teleport-format");
                    return;
                }
                action.setValue(parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3]);
            } else {
                action.setValue(newValue);
            }

            areaManager.saveArea(area);
            MessageUtil.sendConfigMessage(player, "action-updated");
            IAreaCommand.sendActionList(player, area);
        });
    }
}
