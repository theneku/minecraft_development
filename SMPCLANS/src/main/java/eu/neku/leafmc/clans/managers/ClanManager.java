package eu.neku.leafmc.clans.managers;

import eu.neku.leafmc.clans.Loader;
import eu.neku.leafmc.clans.models.Clan;
import eu.neku.leafmc.clans.models.ClanMember;
import eu.neku.leafmc.clans.models.ClanRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings({"SqlNoDataSourceInspection", "unused", "UnusedReturnValue"})
public class ClanManager {
    private final Loader plugin;
    private final Map<Integer, Clan> clansById = new ConcurrentHashMap<>();
    private final Map<String, Clan> clansByName = new ConcurrentHashMap<>();
    private final Map<UUID, Clan> clansByPlayer = new ConcurrentHashMap<>();
    private final Set<UUID> clanChatEnabled = ConcurrentHashMap.newKeySet();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public ClanManager(Loader plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> loadClans() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM clans");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Clan clan = new Clan(
                                rs.getInt("id"), rs.getString("name"),
                                UUID.fromString(rs.getString("leader_uuid")),
                                rs.getInt("level"), rs.getInt("total_kills"), rs.getLong("creation_date")
                        );
                        clansById.put(clan.getId(), clan);
                        clansByName.put(clan.getName().toLowerCase(), clan);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM members");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Clan clan = clansById.get(rs.getInt("clan_id"));
                        if (clan != null) {
                            UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                            clan.addMember(new ClanMember(playerUuid, ClanRole.valueOf(rs.getString("role"))));
                            clansByPlayer.put(playerUuid, clan);
                        }
                    }
                }
                plugin.getLogger().info("Caricati " + clansById.size() + " clan.");
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nel caricamento dei clan: " + e.getMessage());
            }
        }, dbExecutor);
    }

    public Clan getClanById(int id) { return clansById.get(id); }
    public Clan getClanByName(String name) { return clansByName.get(name.toLowerCase()); }
    public Clan getClanByPlayer(UUID playerUuid) { return clansByPlayer.get(playerUuid); }
    public Collection<Clan> getAllClans() { return clansById.values(); }
    public boolean isClanNameTaken(String name) { return clansByName.containsKey(name.toLowerCase()); }

    public CompletableFuture<Boolean> createClan(String name, UUID leaderUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    long creationDate = System.currentTimeMillis();
                    int clanId = -1;

                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO clans (name, leader_uuid, creation_date) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, name);
                        ps.setString(2, leaderUuid.toString());
                        ps.setLong(3, creationDate);
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) clanId = rs.getInt(1);
                        }
                    }

                    if (clanId == -1) {
                        conn.rollback();
                        return false;
                    }

                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO members (clan_id, player_uuid, role) VALUES (?, ?, ?)")) {
                        ps.setInt(1, clanId);
                        ps.setString(2, leaderUuid.toString());
                        ps.setString(3, ClanRole.LEADER.name());
                        ps.executeUpdate();
                    }

                    conn.commit();

                    Clan clan = new Clan(clanId, name, leaderUuid, 1, 0, creationDate);
                    clan.addMember(new ClanMember(leaderUuid, ClanRole.LEADER));
                    clansById.put(clanId, clan);
                    clansByName.put(name.toLowerCase(), clan);
                    clansByPlayer.put(leaderUuid, clan);
                    return true;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nella creazione del clan: " + e.getMessage());
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> deleteClan(Clan clan) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM clans WHERE id = ?")) {
                ps.setInt(1, clan.getId());
                ps.executeUpdate();

                clansById.remove(clan.getId());
                clansByName.remove(clan.getName().toLowerCase());
                for (ClanMember member : clan.getMembers()) {
                    clansByPlayer.remove(member.getPlayerUuid());
                    clanChatEnabled.remove(member.getPlayerUuid());
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nell'eliminazione del clan: " + e.getMessage());
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> addMember(Clan clan, UUID playerUuid, ClanRole role) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO members (clan_id, player_uuid, role) VALUES (?, ?, ?)")) {
                ps.setInt(1, clan.getId());
                ps.setString(2, playerUuid.toString());
                ps.setString(3, role.name());
                ps.executeUpdate();

                clan.addMember(new ClanMember(playerUuid, role));
                clansByPlayer.put(playerUuid, clan);
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nell'aggiunta del membro: " + e.getMessage());
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> removeMember(Clan clan, UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM members WHERE clan_id = ? AND player_uuid = ?")) {
                ps.setInt(1, clan.getId());
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();

                clan.removeMember(playerUuid);
                clansByPlayer.remove(playerUuid);
                clanChatEnabled.remove(playerUuid);
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nella rimozione del membro: " + e.getMessage());
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> updateRole(Clan clan, UUID playerUuid, ClanRole newRole) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE members SET role = ? WHERE clan_id = ? AND player_uuid = ?")) {
                ps.setString(1, newRole.name());
                ps.setInt(2, clan.getId());
                ps.setString(3, playerUuid.toString());
                ps.executeUpdate();

                ClanMember member = clan.getMember(playerUuid);
                if (member != null) member.setRole(newRole);
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nell'aggiornamento del ruolo: " + e.getMessage());
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> saveClanStats(Clan clan) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE clans SET level = ?, total_kills = ? WHERE id = ?")) {
                ps.setInt(1, clan.getLevel());
                ps.setInt(2, clan.getTotalKills());
                ps.setInt(3, clan.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nel salvataggio delle statistiche: " + e.getMessage());
            }
        }, dbExecutor);
    }

    public boolean hasClanChatEnabled(UUID playerUuid) { return clanChatEnabled.contains(playerUuid); }
    public void toggleClanChat(UUID playerUuid) {
        if (!clanChatEnabled.remove(playerUuid)) clanChatEnabled.add(playerUuid);
    }

    public CompletableFuture<Void> invitePlayer(Clan clan, UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                                  PreparedStatement clean = conn.prepareStatement("DELETE FROM invites WHERE timestamp < ?");
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO invites (clan_id, player_uuid, timestamp) VALUES (?, ?, ?)")) {
                clean.setLong(1, System.currentTimeMillis() - 86400000L);
                clean.executeUpdate();
                ps.setInt(1, clan.getId());
                ps.setString(2, playerUuid.toString());
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nell'invito del giocatore: " + e.getMessage());
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> checkAndAcceptInvite(UUID playerUuid, String clanName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                Clan targetClan = getClanByName(clanName);
                if (targetClan == null) return false;

                boolean found = false;
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM invites WHERE clan_id = ? AND player_uuid = ?")) {
                    ps.setInt(1, targetClan.getId());
                    ps.setString(2, playerUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            found = true;
                            int inviteId = rs.getInt("id");
                            try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM invites WHERE id = ?")) {
                                deletePs.setInt(1, inviteId);
                                deletePs.executeUpdate();
                            }
                        }
                    }
                }
                if (found) {
                    try (PreparedStatement psMember = conn.prepareStatement("INSERT INTO members (clan_id, player_uuid, role) VALUES (?, ?, ?)")) {
                        psMember.setInt(1, targetClan.getId());
                        psMember.setString(2, playerUuid.toString());
                        psMember.setString(3, ClanRole.MEMBER.name());
                        psMember.executeUpdate();
                        targetClan.addMember(new ClanMember(playerUuid, ClanRole.MEMBER));
                        clansByPlayer.put(playerUuid, targetClan);
                    }
                    return true;
                }
                return false;
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nell'accettazione dell'invito: " + e.getMessage());
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> requestAlliance(Clan requester, Clan target) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO allies (clan_id_1, clan_id_2, status) VALUES (?, ?, 'pending')")) {
                ps.setInt(1, requester.getId());
                ps.setInt(2, target.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nella richiesta di alleanza: " + e.getMessage());
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> acceptAlliance(Clan clan1, Clan clan2) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE allies SET status = 'accepted' WHERE clan_id_1 = ? AND clan_id_2 = ? AND status = 'pending'")) {
                ps.setInt(1, clan2.getId());
                ps.setInt(2, clan1.getId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore nell'accettazione dell'alleanza: " + e.getMessage());
                return false;
            }
        }, dbExecutor);
    }
}