package net.knawk.mc.smartlibrarians;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

@SuppressWarnings("unused")
public class SmartLibrariansPlugin extends JavaPlugin {
    private final Logger log;

    @SuppressWarnings("unused")
    public SmartLibrariansPlugin() {
        log = getLogger();
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new UpgradeLibrarianTradeListener(log), this);
    }
}
