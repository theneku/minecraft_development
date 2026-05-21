package eu.neku.leafmc.clans.commands;

import eu.neku.leafmc.clans.Loader;
import eu.neku.leafmc.clans.chat.ClanChatManager;
import eu.neku.leafmc.clans.commands.handlers.ClanInfo;
import eu.neku.leafmc.clans.commands.handlers.ClanInteraction;
import eu.neku.leafmc.clans.commands.handlers.ClanManagement;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static eu.neku.leafmc.clans.util.ColorUtil.color;

public class ClanCommand implements CommandExecutor, TabCompleter {
    private final ClanManagement management;
    private final ClanInteraction interaction;
    private final ClanInfo info;
    private final ClanChatManager chat;

    public ClanCommand(Loader plugin, ClanChatManager chatManager) {
        this.management = new ClanManagement(plugin);
        this.interaction = new ClanInteraction(plugin);
        this.info = new ClanInfo(plugin);
        this.chat = chatManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo i giocatori possono usare questo comando.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> management.handleCreate(player, args);
            case "delete" -> management.handleDelete(player);
            case "kick" -> management.handleKick(player, args);
            case "promote" -> management.handlePromote(player, args);
            case "invite" -> interaction.handleInvite(player, args);
            case "accept" -> interaction.handleAccept(player, args);
            case "ally" -> interaction.handleAlly(player, args);
            case "leave" -> interaction.handleLeave(player);
            case "info" -> info.handleInfo(player);
            case "best" -> info.handleBest(player);
            case "chat" -> chat.handleToggleChat(player);
            default -> player.sendMessage(color("&4&l✖ &cComando sconosciuto. Usa &e/clan &cper vedere la lista."));
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&8&m================&r &a&lLEAFMC CLANS &8&m================"));
        player.sendMessage(color(" &7Seleziona uno dei comandi disponibili:"));
        player.sendMessage(color(""));
        player.sendMessage(color(" &e● &7/clan info &8• &fVisualizza le info del tuo clan"));
        player.sendMessage(color(" &e● &7/clan best &8• &fMostra la classifica globale"));
        player.sendMessage(color(" &e● &7/clan chat &8• &fAttiva/Disattiva la chat di clan"));
        player.sendMessage(color(""));
        player.sendMessage(color(" &a● &7/clan create <nome> &8• &fFonda un nuovo clan"));
        player.sendMessage(color(" &a● &7/clan invite <player> &8• &fInvita un membro"));
        player.sendMessage(color(" &a● &7/clan accept <clan> &8• &fAccetta un invito"));
        player.sendMessage(color(" &a● &7/clan leave &8• &fAbbandona il clan attuale"));
        player.sendMessage(color(""));
        player.sendMessage(color(" &c● &7/clan kick <player> &8• &fEspelli un membro"));
        player.sendMessage(color(" &c● &7/clan promote <player> &8• &fPromuovi a Co-Leader"));
        player.sendMessage(color(" &c● &7/clan ally <clan> &8• &fStringi un'alleanza"));
        player.sendMessage(color(" &c● &7/clan delete &8• &fSciogli il tuo clan"));
        player.sendMessage(color("&8&m================================================"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return null;

        if (args.length == 1) {
            return Arrays.asList("create", "delete", "kick", "promote", "invite", "accept", "ally", "leave", "info", "best", "chat");
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "ally" -> Arrays.asList("accept", "<clan>");
                case "create" -> List.of("<nome>");
                case "kick", "promote", "invite" -> null;
                case "accept" -> List.of("<clan>");
                default -> new ArrayList<>();
            };
        }

        return new ArrayList<>();
    }
}