package cz.interactivearea.commands;

import cz.interactivearea.InteractiveAreaPlugin;
import cz.interactivearea.managers.AreaManager;
import cz.interactivearea.managers.VisualizerManager;
import cz.interactivearea.models.Area;
import cz.interactivearea.models.AreaAction;
import cz.interactivearea.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class IAreaCommand implements CommandExecutor, TabCompleter {

    private final InteractiveAreaPlugin plugin;
    private final AreaManager areaManager;
    private final VisualizerManager visualizerManager;

    public IAreaCommand(InteractiveAreaPlugin plugin, AreaManager areaManager, VisualizerManager visualizerManager) {
        this.plugin = plugin;
        this.areaManager = areaManager;
        this.visualizerManager = visualizerManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(MessageUtil.parse("&bInteractiveArea &7- Pouzij &e/iarea <admin|area|tp|area-visualize|reload>"));
            return true;
        }

        String sub = args[0].toLowerCase();

        // /iarea reload
        if (sub.equals("reload")) {
            if (!sender.hasPermission("iarea.reload")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            plugin.reloadConfig();
            areaManager.loadAreas();
            MessageUtil.sendConfigMessage(sender, "reload-success");
            return true;
        }

        // /iarea tp <nazev>
        if (sub.equals("tp")) {
            if (!sender.hasPermission("iarea.tp")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            if (!(sender instanceof Player player)) {
                MessageUtil.sendConfigMessage(sender, "only-players");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(MessageUtil.parse("&cPouzij: /iarea tp <nazev_oblasti>"));
                return true;
            }
            Area area = areaManager.getArea(args[1]);
            if (area == null) {
                MessageUtil.sendConfigMessage(sender, "area-not-found", "%area%", args[1]);
                return true;
            }
            Location center = area.getCenterLocation();
            if (center != null) {
                player.teleport(center);
                MessageUtil.sendConfigMessage(sender, "tp-success", "%area%", area.getName());
            }
            return true;
        }

        // Vsechny ostatni prikazy vyzadují iarea.admin
        if (!sender.hasPermission("iarea.admin")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        // /iarea area-visualize <nazev> <on|off>
        if (sub.equals("area-visualize")) {
            if (args.length < 3) {
                sender.sendMessage(MessageUtil.parse("&cPouzij: /iarea area-visualize <nazev> <on|off>"));
                return true;
            }
            Area area = areaManager.getArea(args[1]);
            if (area == null) {
                MessageUtil.sendConfigMessage(sender, "area-not-found", "%area%", args[1]);
                return true;
            }
            if (args[2].equalsIgnoreCase("on")) {
                visualizerManager.setVisualizing(area.getName(), true);
                MessageUtil.sendConfigMessage(sender, "visualize-on", "%area%", area.getName());
            } else if (args[2].equalsIgnoreCase("off")) {
                visualizerManager.setVisualizing(area.getName(), false);
                MessageUtil.sendConfigMessage(sender, "visualize-off", "%area%", area.getName());
            }
            return true;
        }

        // /iarea admin <create|delete> <nazev>
        if (sub.equals("admin")) {
            if (args.length < 3) {
                sender.sendMessage(MessageUtil.parse("&cPouzij: /iarea admin <create|delete> <nazev>"));
                return true;
            }
            String adminAction = args[1].toLowerCase();
            String areaName = args[2];

            if (adminAction.equals("create")) {
                if (areaManager.getArea(areaName) != null) {
                    MessageUtil.sendConfigMessage(sender, "area-already-exists", "%area%", areaName);
                    return true;
                }
                if (!areaManager.isCreating(areaName)) {
                    areaManager.startCreating(areaName);
                    MessageUtil.sendConfigMessage(sender, "create-started", "%area%", areaName);
                } else {
                    Location p1 = areaManager.getPos1(areaName);
                    Location p2 = areaManager.getPos2(areaName);
                    if (p1 == null || p2 == null || !p1.getWorld().equals(p2.getWorld())) {
                        MessageUtil.sendConfigMessage(sender, "create-missing-pos");
                        return true;
                    }
                    Area newArea = new Area(areaName, p1, p2);
                    areaManager.registerArea(newArea);
                    areaManager.finishCreating(areaName);
                    MessageUtil.sendConfigMessage(sender, "create-success", "%area%", areaName);
                }
                return true;
            }

            if (adminAction.equals("delete")) {
                Area area = areaManager.getArea(areaName);
                if (area == null) {
                    MessageUtil.sendConfigMessage(sender, "area-not-found", "%area%", areaName);
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    areaManager.deleteArea(areaName);
                    MessageUtil.sendConfigMessage(sender, "delete-success", "%area%", areaName);
                    return true;
                }

                AreaManager.DeleteConfirmation conf = areaManager.getDeleteConfirmations().get(player.getUniqueId());
                long now = System.currentTimeMillis();
                if (conf != null && conf.areaName().equalsIgnoreCase(areaName) && (now - conf.timestamp()) <= 5000) {
                    areaManager.getDeleteConfirmations().remove(player.getUniqueId());
                    areaManager.deleteArea(areaName);
                    MessageUtil.sendConfigMessage(sender, "delete-success", "%area%", areaName);
                } else {
                    areaManager.getDeleteConfirmations().put(player.getUniqueId(), new AreaManager.DeleteConfirmation(areaName, now));
                    MessageUtil.sendConfigMessage(sender, "delete-confirm", "%area%", areaName);
                }
                return true;
            }
        }

        // /iarea area <nazev> <pos1|pos2|addaction|action-list|permission|removeaction|editaction>
        if (sub.equals("area")) {
            if (args.length < 3) {
                sender.sendMessage(MessageUtil.parse("&cPouzij: /iarea area <nazev> <pos1|pos2|addaction|action-list|permission>"));
                return true;
            }
            String areaName = args[1];
            String areaSub = args[2].toLowerCase();

            // pos1 a pos2 se zadavaji behem vytvareni (nebo upravy)
            if (areaSub.equals("pos1") || areaSub.equals("pos2")) {
                if (!(sender instanceof Player player)) {
                    MessageUtil.sendConfigMessage(sender, "only-players");
                    return true;
                }
                if (areaSub.equals("pos1")) {
                    areaManager.setPos1(areaName, player.getLocation());
                    MessageUtil.sendConfigMessage(sender, "pos1-set", "%area%", areaName);
                } else {
                    areaManager.setPos2(areaName, player.getLocation());
                    MessageUtil.sendConfigMessage(sender, "pos2-set", "%area%", areaName);
                }
                return true;
            }

            Area area = areaManager.getArea(areaName);
            if (area == null) {
                MessageUtil.sendConfigMessage(sender, "area-not-found", "%area%", areaName);
                return true;
            }

            // /iarea area <nazev> permission <permise>
            if (areaSub.equals("permission")) {
                if (args.length < 4) {
                    sender.sendMessage(MessageUtil.parse("&cPouzij: /iarea area " + area.getName() + " permission <permise>"));
                    return true;
                }
                area.setPermission(args[3]);
                areaManager.saveArea(area);
                MessageUtil.sendConfigMessage(sender, "permission-set", "%area%", area.getName(), "%permission%", args[3]);
                return true;
            }

            // /iarea area <nazev> addaction ...
            if (areaSub.equals("addaction")) {
                if (args.length < 4) {
                    sender.sendMessage(MessageUtil.parse("&cPouzij: /iarea area " + area.getName() + " addaction <teleport|commands|text> ..."));
                    return true;
                }
                String actionCategory = args[3].toLowerCase();

                if (actionCategory.equals("teleport")) {
                    if (!(sender instanceof Player player)) {
                        MessageUtil.sendConfigMessage(sender, "only-players");
                        return true;
                    }
                    Location loc = player.getLocation();
                    String val = String.format(Locale.US, "%s %.2f %.2f %.2f %.2f %.2f",
                            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                    area.getActions().add(new AreaAction(AreaAction.ActionType.TELEPORT, val));
                    areaManager.saveArea(area);
                    MessageUtil.sendConfigMessage(sender, "action-added", "%type%", "teleport", "%area%", area.getName());
                    return true;
                }

                if (actionCategory.equals("commands")) {
                    if (args.length < 6) {
                        sender.sendMessage(MessageUtil.parse("&cPouzij: /iarea area " + area.getName() + " addaction commands <player|console> <prikaz bez />"));
                        return true;
                    }
                    AreaAction.CommandExecutorType execType = args[4].equalsIgnoreCase("player")
                            ? AreaAction.CommandExecutorType.PLAYER : AreaAction.CommandExecutorType.CONSOLE;
                    String cmd = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
                    if (cmd.startsWith("/")) cmd = cmd.substring(1);

                    AreaAction action = new AreaAction(AreaAction.ActionType.COMMAND, cmd);
                    action.setExecutorType(execType);
                    area.getActions().add(action);
                    areaManager.saveArea(area);
                    MessageUtil.sendConfigMessage(sender, "action-added", "%type%", "command", "%area%", area.getName());
                    return true;
                }

                if (actionCategory.equals("text")) {
                    if (args.length < 6) {
                        sender.sendMessage(MessageUtil.parse("&cPouzij: /iarea area " + area.getName() + " addaction text <brodcast|message> <zprava>"));
                        return true;
                    }
                    String textMode = args[4].toLowerCase();
                    AreaAction.ActionType textType = (textMode.equals("broadcast") || textMode.equals("brodcast"))
                            ? AreaAction.ActionType.TEXT_BROADCAST : AreaAction.ActionType.TEXT_MESSAGE;
                    String msg = String.join(" ", Arrays.copyOfRange(args, 5, args.length));

                    area.getActions().add(new AreaAction(textType, msg));
                    areaManager.saveArea(area);
                    MessageUtil.sendConfigMessage(sender, "action-added", "%type%", textType.getDisplayName(), "%area%", area.getName());
                    return true;
                }
            }

            // /iarea area <nazev> action-list
            if (areaSub.equals("action-list")) {
                sendActionList(sender, area);
                return true;
            }

            // Interni prikaz pro smazani kliknutim na (-)
            if (areaSub.equals("removeaction") && args.length >= 4) {
                AreaAction toRemove = area.getActionById(args[3]);
                if (toRemove != null) {
                    area.getActions().remove(toRemove);
                    areaManager.saveArea(area);
                    MessageUtil.sendConfigMessage(sender, "action-removed", "%area%", area.getName());
                    sendActionList(sender, area);
                }
                return true;
            }

            // Interni prikaz pro zahajeni editace kliknutim na %value%
            if (areaSub.equals("editaction") && args.length >= 4 && sender instanceof Player player) {
                AreaAction toEdit = area.getActionById(args[3]);
                if (toEdit != null) {
                    areaManager.getPendingEdits().put(player.getUniqueId(), new AreaManager.PendingEdit(area.getName(), toEdit.getId()));
                    player.sendMessage(MessageUtil.parse("&eNapis novou hodnotu do chatu a odesli ji (Enter)."));
                }
                return true;
            }
        }

        return true;
    }

    public static void sendActionList(CommandSender sender, Area area) {
        InteractiveAreaPlugin plugin = InteractiveAreaPlugin.getInstance();
        String header = plugin.getConfig().getString("messages.action-list-header", "&bAkce dane oblasti jsou:");
        sender.sendMessage(MessageUtil.parse(header));

        if (area.getActions().isEmpty()) {
            String empty = plugin.getConfig().getString("messages.action-list-empty", "&7- &oZadne akce nejsou nastaveny.");
            sender.sendMessage(MessageUtil.parse(empty));
            return;
        }

        String lineFormat = plugin.getConfig().getString("messages.action-list-line", "&7- &e%type% %value% &c(-)");

        for (AreaAction action : area.getActions()) {
            String typeStr = action.getType().getDisplayName();
            String displayVal = action.getType() == AreaAction.ActionType.TELEPORT
                    ? action.getCleanTeleportSuggest()
                    : action.getValue();

            // Rozdeleni radku podle placeholderu pro presne prirazeni ClickEventu
            String[] splitType = lineFormat.split("%type%", 2);
            String beforeType = splitType[0];
            String afterType = splitType.length > 1 ? splitType[1] : "";

            String[] splitVal = afterType.split("%value%", 2);
            String betweenTypeAndVal = splitVal[0];
            String afterVal = splitVal.length > 1 ? splitVal[1] : "";

            String[] splitRemove = afterVal.split("\\(-\\)", 2);
            String beforeRemove = splitRemove[0];
            String afterRemove = splitRemove.length > 1 ? splitRemove[1] : "";

            // 1. Cast pred typem + samotny %type% (bez klikaci akce)
            Component lineComp = MessageUtil.parse(beforeType + typeStr + betweenTypeAndVal);

            // 2. Cast %value% (kliknutim doplni hodnotu do chatu + aktivuje rezim editace)
            String suggestValue = action.getType() == AreaAction.ActionType.TELEPORT
                    ? action.getCleanTeleportSuggest()
                    : action.getValue();

            Component valueComp = MessageUtil.parse(displayVal)
                    .hoverEvent(HoverEvent.showText(MessageUtil.parse("&aKlikni pro upravu hodnoty")))
                    .clickEvent(ClickEvent.suggestCommand(suggestValue));

            // Aktivace editacniho rezimu pri kliknuti na value pomoci run_command neni mozna soucasne se suggest_command,
            // proto pouzijeme trik: kliknutim na value se spusti prikaz, ktery hraci posle klikaci zpravu nebo rovnou nastavi rezim.
            // V Paper API lze na jednu komponentu dat pouze 1 ClickEvent. Nejlepsi UX:
            // Kliknutim na %value% se spusti /iarea area <nazev> editaction <id> a zaroven se doplni text pres chat prompt,
            // NEBO suggestCommand primo doplni text a hrac pred nej nic nepise - zde pouzijeme ciste suggestCommand
            // a editaci navazeme pres klikaci komponentu:
            valueComp = Component.empty()
                    .append(MessageUtil.parse(displayVal))
                    .hoverEvent(HoverEvent.showText(MessageUtil.parse("&eKlikni pro upravu hodnoty")))
                    .clickEvent(ClickEvent.runCommand("/iarea area " + area.getName() + " editaction " + action.getId()));

            // 3. Cast (-) pro okamzite smazani
            Component removeComp = MessageUtil.parse(beforeRemove)
                    .append(MessageUtil.parse("&c(-)")
                            .hoverEvent(HoverEvent.showText(MessageUtil.parse("&cKlikni pro smazani akce")))
                            .clickEvent(ClickEvent.runCommand("/iarea area " + area.getName() + " removeaction " + action.getId())))
                    .append(MessageUtil.parse(afterRemove));

            sender.sendMessage(lineComp.append(valueComp).append(removeComp));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("iarea.admin")) {
                completions.addAll(Arrays.asList("admin", "area", "area-visualize"));
            }
            if (sender.hasPermission("iarea.tp")) {
                completions.add("tp");
            }
            if (sender.hasPermission("iarea.reload")) {
                completions.add("reload");
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("admin") && sender.hasPermission("iarea.admin")) {
                completions.addAll(Arrays.asList("create", "delete"));
            } else if ((sub.equals("area") || sub.equals("area-visualize")) && sender.hasPermission("iarea.admin")) {
                completions.addAll(getAreaNames());
            } else if (sub.equals("tp") && sender.hasPermission("iarea.tp")) {
                completions.addAll(getAreaNames());
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("admin") && args[1].equalsIgnoreCase("delete") && sender.hasPermission("iarea.admin")) {
                completions.addAll(getAreaNames());
            } else if (sub.equals("area") && sender.hasPermission("iarea.admin")) {
                completions.addAll(Arrays.asList("pos1", "pos2", "addaction", "action-list", "permission"));
            } else if (sub.equals("area-visualize") && sender.hasPermission("iarea.admin")) {
                completions.addAll(Arrays.asList("on", "off"));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("area") && args[2].equalsIgnoreCase("addaction") && sender.hasPermission("iarea.admin")) {
                completions.addAll(Arrays.asList("teleport", "commands", "text"));
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("area") && args[2].equalsIgnoreCase("addaction") && sender.hasPermission("iarea.admin")) {
                if (args[3].equalsIgnoreCase("commands")) {
                    completions.addAll(Arrays.asList("player", "console"));
                } else if (args[3].equalsIgnoreCase("text")) {
                    completions.addAll(Arrays.asList("brodcast", "message"));
                }
            }
        }

        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }

    private List<String> getAreaNames() {
        return areaManager.getAllAreas().stream().map(Area::getName).collect(Collectors.toList());
    }
}
