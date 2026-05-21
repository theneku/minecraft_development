package eu.neku.leafmc.clans.chat;

import eu.neku.leafmc.clans.Loader;
import eu.neku.leafmc.clans.models.Clan;
import eu.neku.leafmc.clans.models.ClanMember;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClanChatManager implements Listener {
    private final Loader plugin;

    public ClanChatManager(Loader plugin) {
        this.plugin = plugin;
    }

    public void handleToggleChat(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(Component.text("Non sei in nessun clan!", NamedTextColor.RED));
            return;
        }

        plugin.getClanManager().toggleClanChat(player.getUniqueId());
        
        if (plugin.getClanManager().hasClanChatEnabled(player.getUniqueId())) {
            player.sendMessage(Component.text("Hai ATTIVATO la chat di clan. I tuoi messaggi saranno visti solo dai membri.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Hai DISATTIVATO la chat di clan. I tuoi messaggi torneranno pubblici.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.getClanManager().hasClanChatEnabled(player.getUniqueId())) {
            Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
            
            if (clan != null) {
                event.setCancelled(true);
                
                String message = PlainTextComponentSerializer.plainText().serialize(event.message());
                Component formattedMessage = Component.text("[CLAN] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(player.getName() + ": ", NamedTextColor.YELLOW))
                    .append(Component.text(message, NamedTextColor.WHITE));
                
                for (ClanMember member : clan.getMembers()) {
                    Player clanPlayer = Bukkit.getPlayer(member.getPlayerUuid());
                    if (clanPlayer != null && clanPlayer.isOnline()) {
                        clanPlayer.sendMessage(formattedMessage);
                    }
                }
            } else {
                plugin.getClanManager().toggleClanChat(player.getUniqueId());
            }
        }
    }
}
