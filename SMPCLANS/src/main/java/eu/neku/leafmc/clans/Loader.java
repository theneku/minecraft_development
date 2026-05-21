package eu.neku.leafmc.clans;

import eu.neku.leafmc.clans.chat.ClanChatManager;
import eu.neku.leafmc.clans.commands.ClanCommand;
import eu.neku.leafmc.clans.database.DatabaseManager;
import eu.neku.leafmc.clans.listeners.PlayerDeathListener;
import eu.neku.leafmc.clans.managers.ClanManager;
import eu.neku.leafmc.clans.menu.ClanMenu;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Loader extends JavaPlugin {
    private DatabaseManager databaseManager;
    private ClanManager clanManager;

    @Override
    public void onEnable() {
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        clanManager = new ClanManager(this);
        clanManager.loadClans().join();

        ClanChatManager chatManager = new ClanChatManager(this);

        PluginCommand clanCmd = getCommand("clan");
        if (clanCmd != null) {
            ClanCommand executor = new ClanCommand(this, chatManager);
            clanCmd.setExecutor(executor);
            clanCmd.setTabCompleter(executor);
        } else {
            getLogger().warning("Comando non trovato!");
        }

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(chatManager, this);
        getServer().getPluginManager().registerEvents(new ClanMenu(this), this);

        getLogger().info("Plugin avviato!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }
}