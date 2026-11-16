package cz.interactivearea;

import cz.interactivearea.commands.IAreaCommand;
import cz.interactivearea.listeners.ChatEditListener;
import cz.interactivearea.listeners.PlayerMoveListener;
import cz.interactivearea.managers.AreaManager;
import cz.interactivearea.managers.VisualizerManager;
import cz.interactivearea.utils.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class InteractiveAreaPlugin extends JavaPlugin {

    private static InteractiveAreaPlugin instance;
    private AreaManager areaManager;
    private VisualizerManager visualizerManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        MessageUtil.init(this);
        this.areaManager = new AreaManager(this);
        this.visualizerManager = new VisualizerManager(this, areaManager);

        this.areaManager.loadAreas();
        this.visualizerManager.startTask();

        IAreaCommand commandExecutor = new IAreaCommand(this, areaManager, visualizerManager);
        if (getCommand("iarea") != null) {
            getCommand("iarea").setExecutor(commandExecutor);
            getCommand("iarea").setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(new PlayerMoveListener(areaManager), this);
        getServer().getPluginManager().registerEvents(new ChatEditListener(this, areaManager), this);

        getLogger().info("InteractiveArea byl úspěšně aktivován!");
    }

    @Override
    public void onDisable() {
        if (visualizerManager != null) {
            visualizerManager.stopTask();
        }
        getLogger().info("InteractiveArea byl deaktivován.");
    }

    public static InteractiveAreaPlugin getInstance() {
        return instance;
    }

    public AreaManager getAreaManager() {
        return areaManager;
    }

    public VisualizerManager getVisualizerManager() {
        return visualizerManager;
    }
}
