package eu.neku.leafmc.clans.models;

import java.util.UUID;

public class ClanMember {
    private final UUID playerUuid;
    private ClanRole role;

    public ClanMember(UUID playerUuid, ClanRole role) {
        this.playerUuid = playerUuid;
        this.role = role;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public ClanRole getRole() {
        return role;
    }

    public void setRole(ClanRole role) {
        this.role = role;
    }
}
