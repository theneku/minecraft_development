package eu.neku.leafmc.clans.commands.handlers;

import eu.neku.leafmc.clans.Loader;
import eu.neku.leafmc.clans.models.Clan;
import eu.neku.leafmc.clans.models.ClanMember;
import eu.neku.leafmc.clans.models.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static eu.neku.leafmc.clans.util.ColorUtil.color;

public class ClanManagement {
    private final Loader plugin;
    private final Map<UUID, Long> confirmMap = new HashMap<>();

    public ClanManagement(Loader plugin) {
        this.plugin = plugin;
    }

    public void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUso corretto: /clan create <nome>"));
            return;
        }

        if (plugin.getClanManager().getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(color("&cSei già in un clan! Devi prima uscirne."));
            return;
        }

        String clanName = args[1];
        if (!clanName.matches("^[a-zA-Z0-9_]{3,16}$")) {
            player.sendMessage(color("&cIl nome del clan deve essere tra 3 e 16 caratteri alfanumerici."));
            return;
        }

        if (plugin.getClanManager().isClanNameTaken(clanName)) {
            player.sendMessage(color("&cQuesto nome è già in uso."));
            return;
        }

        player.sendMessage(color("&eCreazione del clan in corso..."));

        plugin.getClanManager().createClan(clanName, player.getUniqueId()).thenAccept(success ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(color("&aHai creato con successo il clan " + clanName + "!"));
                    } else {
                        player.sendMessage(color("&cErrore durante la creazione del clan."));
                    }
                })
        );
    }

    public void handleDelete(Player player) {
        confirmMap.entrySet().removeIf(entry -> (System.currentTimeMillis() - entry.getValue()) >= 15000);

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(color("&cNon sei in nessun clan!"));
            return;
        }

        if (!clan.getLeaderUuid().equals(player.getUniqueId())) {
            player.sendMessage(color("&cSolo il leader può eliminare il clan."));
            return;
        }

        UUID uuid = player.getUniqueId();
        if (confirmMap.containsKey(uuid) && (System.currentTimeMillis() - confirmMap.get(uuid)) < 15000) {
            confirmMap.remove(uuid);
            player.sendMessage(color("&cEliminazione del clan in corso..."));
            plugin.getClanManager().deleteClan(clan).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(color("&aIl clan " + clan.getName() + " è stato eliminato."))
                    )
            );
        } else {
            confirmMap.put(uuid, System.currentTimeMillis());
            player.sendMessage(color("&cSei sicuro di voler eliminare il clan? Questa azione è irreversibile!"));
            player.sendMessage(color("&cDigita di nuovo /clan delete entro 15 secondi per confermare."));
        }
    }

    public void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUso corretto: /clan kick <giocatore>"));
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(color("&cNon sei in nessun clan!"));
            return;
        }

        ClanMember executorMember = clan.getMember(player.getUniqueId());
        if (executorMember.getRole() == ClanRole.MEMBER) {
            player.sendMessage(color("&cSolo leader e co-leader possono espellere i giocatori."));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        ClanMember targetMember = clan.getMember(target.getUniqueId());

        if (targetMember == null) {
            player.sendMessage(color("&cQuesto giocatore non è nel tuo clan."));
            return;
        }

        if (targetMember.getRole() == ClanRole.LEADER) {
            player.sendMessage(color("&cNon puoi espellere il leader!"));
            return;
        }

        if (executorMember.getRole() == ClanRole.CO_LEADER && targetMember.getRole() == ClanRole.CO_LEADER) {
            player.sendMessage(color("&cUn co-leader non può espellere un altro co-leader."));
            return;
        }

        plugin.getClanManager().removeMember(clan, target.getUniqueId()).thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(color("&aHai espulso " + target.getName() + " dal clan."));
                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer().sendMessage(color("&cSei stato espulso dal clan " + clan.getName() + "."));
                    }
                })
        );
    }

    public void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUso corretto: /clan promote <giocatore>"));
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(color("&cNon sei in nessun clan!"));
            return;
        }

        ClanMember executorMember = clan.getMember(player.getUniqueId());
        if (executorMember.getRole() != ClanRole.LEADER) {
            player.sendMessage(color("&cSolo il leader può promuovere i membri."));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        ClanMember targetMember = clan.getMember(target.getUniqueId());

        if (targetMember == null) {
            player.sendMessage(color("&cQuesto giocatore non è nel tuo clan."));
            return;
        }

        if (targetMember.getRole() != ClanRole.MEMBER) {
            player.sendMessage(color("&cQuesto giocatore è già " + targetMember.getRole().name() + "."));
            return;
        }

        plugin.getClanManager().updateRole(clan, target.getUniqueId(), ClanRole.CO_LEADER).thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(color("&aHai promosso " + target.getName() + " a Co-Leader!"));
                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer().sendMessage(color("&aSei stato promosso a Co-Leader!"));
                    }
                })
        );
    }
}