package eu.neku.leafmc.clans.listeners;

import eu.neku.leafmc.clans.Loader;
import eu.neku.leafmc.clans.models.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import static eu.neku.leafmc.clans.util.ColorUtil.color;

public class PlayerDeathListener implements Listener {
    private final Loader plugin;

    public PlayerDeathListener(Loader plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && killer != victim) {
            Clan killerClan = plugin.getClanManager().getClanByPlayer(killer.getUniqueId());
            Clan victimClan = plugin.getClanManager().getClanByPlayer(victim.getUniqueId());

            if (killerClan != null) {
                if (victimClan != null && victimClan.getId() == killerClan.getId()) return;

                killerClan.addKills(1);

                if (killerClan.checkLevelUp()) {
                    plugin.getServer().sendMessage(color("&8[&a&lLeafMC Clans&8] &fIl clan &e" + killerClan.getName() + " &fha raggiunto il livello &a" + killerClan.getLevel() + "&f!"));
                }

                plugin.getClanManager().saveClanStats(killerClan);
            }
        }
    }
}