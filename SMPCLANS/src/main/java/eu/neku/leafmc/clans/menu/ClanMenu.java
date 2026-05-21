package eu.neku.leafmc.clans.menu;

import eu.neku.leafmc.clans.Loader;
import eu.neku.leafmc.clans.models.Clan;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static eu.neku.leafmc.clans.util.ColorUtil.color;

public class ClanMenu implements Listener {
    private final Loader plugin;
    private final Component inventoryTitle = color("&8Top 3 Clans");

    public ClanMenu(Loader plugin) {
        this.plugin = plugin;
    }

    public void openLeaderboard(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, inventoryTitle);

        List<Clan> sortedClans = new ArrayList<>(plugin.getClanManager().getAllClans());
        sortedClans.sort(Comparator.comparingInt(Clan::getTotalKills).reversed());

        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(Component.empty());
            bg.setItemMeta(bgMeta);
        }

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, bg);
        }

        if (!sortedClans.isEmpty()) {
            inventory.setItem(13, createClanItem(sortedClans.getFirst(), Material.DIAMOND_BLOCK, "&b&l#1 "));
        }
        if (sortedClans.size() > 1) {
            inventory.setItem(11, createClanItem(sortedClans.get(1), Material.GOLD_BLOCK, "&6&l#2 "));
        }
        if (sortedClans.size() > 2) {
            inventory.setItem(15, createClanItem(sortedClans.get(2), Material.IRON_BLOCK, "&f&l#3 "));
        }

        player.openInventory(inventory);
    }

    private ItemStack createClanItem(Clan clan, Material material, String prefix) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(color(prefix + "&a" + clan.getName()));

            List<Component> lore = new ArrayList<>();
            lore.add(color("&8&m-------------------------"));
            lore.add(color(" &7Informazioni Generali:"));

            String leaderName = "Sconosciuto";
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(clan.getLeaderUuid());
            if (op.getName() != null) leaderName = op.getName();

            meta.displayName(color(prefix + " &a&l" + clan.getName()));
            lore.add(color("   &e• &7Leader: &f" + leaderName));
            lore.add(color("   &e• &7Livello Fazione: &b" + clan.getLevel()));
            lore.add(color("   &e• &7Componenti: &f" + clan.getMembers().size() + " utenti"));
            lore.add(color(""));
            lore.add(color(" &7Statistiche di Guerra:"));
            lore.add(color("   &c• &7Uccisioni Totali: &e" + clan.getTotalKills() + " &c⚔"));
            lore.add(color("&8&m-------------------------"));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(inventoryTitle)) {
            event.setCancelled(true);
        }
    }
}