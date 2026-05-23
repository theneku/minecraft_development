package eu.neku.leafmc.clans.models;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unused")
public class Clan {
    private final int id;
    private final String name;
    private final long creationDate;

    private UUID leaderUuid;
    private int level;
    private int totalKills;

    private final List<ClanMember> members;

    public Clan(int id, String name, UUID leaderUuid, int level, int totalKills, long creationDate) {
        this.id = id;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.level = level;
        this.totalKills = totalKills;
        this.creationDate = creationDate;
        this.members = new CopyOnWriteArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
    }

    public void addKills(int amount) {
        this.totalKills += amount;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public List<ClanMember> getMembers() {
        return members;
    }

    public void addMember(ClanMember member) {
        this.members.add(member);
    }

    public void removeMember(UUID uuid) {
        this.members.removeIf(m -> m.getPlayerUuid().equals(uuid));
    }

    public ClanMember getMember(UUID uuid) {
        return members.stream().filter(m -> m.getPlayerUuid().equals(uuid)).findFirst().orElse(null);
    }

    public boolean hasMember(UUID uuid) {
        return getMember(uuid) != null;
    }

    public int getMaxMembers() {
        return switch (level) {
            case 1 -> 15;
            case 2 -> 25;
            case 3 -> 40;
            case 4 -> 60;
            default -> 100;
        };
    }

    public int getRequiredKillsForNextLevel() {
        return switch (level) {
            case 1 -> 100;
            case 2 -> 300;
            case 3 -> 600;
            case 4 -> 1000;
            default -> -1;
        };
    }

    public boolean checkLevelUp() {
        int req = getRequiredKillsForNextLevel();
        if (req != -1 && totalKills >= req) {
            level++;
            return true;
        }
        return false;
    }
}