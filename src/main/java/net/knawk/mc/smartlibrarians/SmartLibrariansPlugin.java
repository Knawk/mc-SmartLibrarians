package net.knawk.mc.smartlibrarians;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class SmartLibrariansPlugin extends JavaPlugin {
    private final Logger log;

    public SmartLibrariansPlugin() {
        log = getLogger();
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new UpgradeLibrarianTradeListener(log), this);
        log.info("SmartLibrarians enabled");
    }

    @Override
    public void onDisable() {
        log.info("SmartLibrarians disabled");
    }
}
