package eu.neku.leafmc.clans.database;

import eu.neku.leafmc.clans.Loader;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final Loader plugin;
    private Connection connection;

    public DatabaseManager(Loader plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().severe("Impossibile creare la cartella dati del plugin!");
                return;
            }

            File databaseFile = new File(dataFolder, "clans.db");
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = 5000;");
            }


            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Impossibile connettersi al database SQLite: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Impossibile disconnettersi dal database SQLite: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore nel controllo della connessione al database: " + e.getMessage());
        }
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return method.invoke(connection, args);
                });
    }

    @SuppressWarnings("SqlNoDataSourceInspection")
    private void createTables() {
        String createClansTable = "CREATE TABLE IF NOT EXISTS clans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE COLLATE NOCASE," +
                "leader_uuid TEXT NOT NULL," +
                "level INTEGER DEFAULT 1," +
                "total_kills INTEGER DEFAULT 0," +
                "creation_date INTEGER NOT NULL" +
                ");";

        String createMembersTable = "CREATE TABLE IF NOT EXISTS members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "clan_id INTEGER NOT NULL," +
                "player_uuid TEXT NOT NULL UNIQUE," +
                "role TEXT NOT NULL," +
                "FOREIGN KEY(clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                ");";

        String createAlliesTable = "CREATE TABLE IF NOT EXISTS allies (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "clan_id_1 INTEGER NOT NULL," +
                "clan_id_2 INTEGER NOT NULL," +
                "status TEXT NOT NULL," +
                "FOREIGN KEY(clan_id_1) REFERENCES clans(id) ON DELETE CASCADE," +
                "FOREIGN KEY(clan_id_2) REFERENCES clans(id) ON DELETE CASCADE" +
                ");";

        String createInvitesTable = "CREATE TABLE IF NOT EXISTS invites (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "clan_id INTEGER NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "timestamp INTEGER NOT NULL," +
                "FOREIGN KEY(clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute(createClansTable);
            statement.execute(createMembersTable);
            statement.execute(createAlliesTable);
            statement.execute(createInvitesTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore nella creazione delle tabelle del database: " + e.getMessage());
        }
    }
}
