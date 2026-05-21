package eu.neku.leafmc.clans.commands.handlers;

import eu.neku.leafmc.clans.Loader;
import eu.neku.leafmc.clans.models.Clan;
import eu.neku.leafmc.clans.models.ClanMember;
import eu.neku.leafmc.clans.models.ClanRole;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static eu.neku.leafmc.clans.util.ColorUtil.color;

public class ClanInteraction {
    private final Loader plugin;

    public ClanInteraction(Loader plugin) {
        this.plugin = plugin;
    }

    public void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.empty());
            player.sendMessage(color("&4&l✖ &cUso corretto: &e/clan invite <giocatore>"));
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(color("&4&l✖ &cDevi far parte di un clan per poter invitare qualcuno."));
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRole() == ClanRole.MEMBER) {
            player.sendMessage(color("&4&l✖ &cPermessi insufficienti. Solo Leader e Co-Leader possono invitare."));
            return;
        }

        if (clan.getMembers().size() >= clan.getMaxMembers()) {
            player.sendMessage(color("&4&l✖ &cIl clan è pieno! Sblocca altri slot aumentando di livello."));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(color("&4&l✖ &cIl giocatore specificato non è online."));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(color("&4&l✖ &cNon puoi autoinvitarti."));
            return;
        }

        if (plugin.getClanManager().getClanByPlayer(target.getUniqueId()) != null) {
            player.sendMessage(color("&4&l✖ &cQuesto giocatore fa già parte di un altro clan."));
            return;
        }

        plugin.getClanManager().invitePlayer(clan, target.getUniqueId()).thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(color("&4&l✔ &aInvito spedito con successo a &e" + target.getName() + "&a."));

                    Component inviteMsg = color("\n&8&m====================================\n" +
                            " &6&lINVITO RICEVUTO\n" +
                            " &7Il clan &a" + clan.getName() + " &7ti ha invitato ad unirti!\n" +
                            "\n" +
                            " &a&l[CLICCA QUI PER ACCETTARE L'INVITO]\n" +
                            "&8&m====================================\n")
                            .clickEvent(ClickEvent.runCommand("/clan accept " + clan.getName()));
                    target.sendMessage(inviteMsg);
                })
        );
    }

    public void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUso corretto: /clan accept <clan>"));
            return;
        }

        if (plugin.getClanManager().getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(color("&cSei già in un clan!"));
            return;
        }

        String clanName = args[1];
        player.sendMessage(color("&eElaborazione invito..."));

        plugin.getClanManager().checkAndAcceptInvite(player.getUniqueId(), clanName).thenAccept(success ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(color("&aSei entrato con successo nel clan " + clanName + "!"));
                    } else {
                        player.sendMessage(color("&cNessun invito valido trovato o il clan non esiste più."));
                    }
                })
        );
    }

    public void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUso corretto: /clan ally <clan|accept> [clan]"));
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(color("&cNon sei in nessun clan!"));
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRole() != ClanRole.LEADER) {
            player.sendMessage(color("&cSolo il leader può gestire le alleanze."));
            return;
        }

        if (args[1].equalsIgnoreCase("accept")) {
            if (args.length < 3) {
                player.sendMessage(color("&cUso corretto: /clan ally accept <clan>"));
                return;
            }

            Clan targetClan = plugin.getClanManager().getClanByName(args[2]);
            if (targetClan == null) {
                player.sendMessage(color("&cClan non trovato."));
                return;
            }

            plugin.getClanManager().acceptAlliance(clan, targetClan).thenAccept(success ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            player.sendMessage(color("&aAlleanza accettata con " + targetClan.getName() + "!"));
                            Player targetLeader = Bukkit.getPlayer(targetClan.getLeaderUuid());
                            if (targetLeader != null) {
                                targetLeader.sendMessage(color("&aIl clan " + clan.getName() + " ha accettato l'alleanza!"));
                            }
                        } else {
                            player.sendMessage(color("&cNessuna richiesta trovata."));
                        }
                    })
            );
            return;
        }

        Clan targetClan = plugin.getClanManager().getClanByName(args[1]);
        if (targetClan == null) {
            player.sendMessage(color("&cClan non trovato."));
            return;
        }

        if (targetClan.getId() == clan.getId()) {
            player.sendMessage(color("&cNon puoi allearti con te stesso!"));
            return;
        }

        plugin.getClanManager().requestAlliance(clan, targetClan).thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(color("&aRichiesta inviata a " + targetClan.getName() + "!"));
                    Player targetLeader = Bukkit.getPlayer(targetClan.getLeaderUuid());
                    if (targetLeader != null) {
                        Component inviteMsg = color("&eIl clan &6" + clan.getName() + " &eha richiesto un'alleanza!\n&a[CLICCA PER ACCETTARE]")
                                .clickEvent(ClickEvent.runCommand("/clan ally accept " + clan.getName()));
                        targetLeader.sendMessage(inviteMsg);
                    }
                })
        );
    }

    public void handleLeave(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(color("&cNon sei in nessun clan!"));
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRole() == ClanRole.LEADER) {
            player.sendMessage(color("&cIl leader non può abbandonare. Usa /clan delete per chiudere il clan."));
            return;
        }

        plugin.getClanManager().removeMember(clan, player.getUniqueId()).thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(color("&aHai abbandonato il clan " + clan.getName() + "."))
                )
        );
    }
}