package eu.neku.leafmc.clans.commands.handlers;

import eu.neku.leafmc.clans.Loader;
import eu.neku.leafmc.clans.menu.ClanMenu;
import eu.neku.leafmc.clans.models.Clan;
import org.bukkit.entity.Player;

import static eu.neku.leafmc.clans.util.ColorUtil.color;

public class ClanInfo {
    private final Loader plugin;

    public ClanInfo(Loader plugin) {
        this.plugin = plugin;
    }

    public void handleInfo(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(color("&cNon appartieni a nessun clan. Fondane uno con &e/clan create&c!"));
            return;
        }

        String killsText = clan.getTotalKills() + (clan.getRequiredKillsForNextLevel() != -1 ? " &8/ &e" + clan.getRequiredKillsForNextLevel() : " &a&l(MAX)");

        player.sendMessage(color("&8&m================&r &e&lINFO CLAN &8&m================"));
        player.sendMessage(color(" &7Nome Clan: &a&l" + clan.getName()));
        player.sendMessage(color(" &7Livello: &e" + clan.getLevel()));
        player.sendMessage(color(""));
        player.sendMessage(color(" &7Membri totali: &f" + clan.getMembers().size() + "&8/&a" + clan.getMaxMembers()));
        player.sendMessage(color(" &7Uccisioni: &f" + killsText));
        player.sendMessage(color("&8&m================================================"));
    }

    public void handleBest(Player player) {
        new ClanMenu(plugin).openLeaderboard(player);
    }
}