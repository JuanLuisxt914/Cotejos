package com.cotejador.app.data.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final String CREATE_EVENTOS_TABLE =
            "CREATE TABLE IF NOT EXISTS eventos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT NOT NULL, " +
                    "fecha TEXT NOT NULL, " +
                    "modalidad TEXT NOT NULL" +
                    ")";
    private static final String CREATE_PARTIDOS_TABLE =
            "CREATE TABLE IF NOT EXISTS partidos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT NOT NULL, " +
                    "evento_id INTEGER NOT NULL, " +
                    "FOREIGN KEY (evento_id) REFERENCES eventos(id) ON DELETE CASCADE" +
                    ")";
    private static final String CREATE_GALLOS_TABLE =
            "CREATE TABLE IF NOT EXISTS gallos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT NOT NULL, " +
                    "peso REAL NOT NULL CHECK (peso > 0), " +
                    "anillo TEXT, " +
                    "partido_id INTEGER NOT NULL, " +
                    "FOREIGN KEY (partido_id) REFERENCES partidos(id) ON DELETE CASCADE" +
                    ")";

    private final SQLiteConnectionFactory connectionFactory;

    public DatabaseInitializer(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_EVENTOS_TABLE);
            statement.execute(CREATE_PARTIDOS_TABLE);
            statement.execute(CREATE_GALLOS_TABLE);
            if (schemaNeedsMigration(connection)) {
                migrateSchema(connection);
            }
        }
    }

    private boolean schemaNeedsMigration(Connection connection) throws SQLException {
        return !hasForeignKeys(connection, "partidos") || !hasForeignKeys(connection, "gallos");
    }

    private boolean hasForeignKeys(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_list(" + tableName + ")")) {
            return resultSet.next();
        }
    }

    private void migrateSchema(Connection connection) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            connection.setAutoCommit(false);

            statement.execute("ALTER TABLE eventos RENAME TO eventos_old");
            statement.execute("ALTER TABLE partidos RENAME TO partidos_old");
            statement.execute("ALTER TABLE gallos RENAME TO gallos_old");

            statement.execute(CREATE_EVENTOS_TABLE);
            statement.execute(CREATE_PARTIDOS_TABLE);
            statement.execute(CREATE_GALLOS_TABLE);

            statement.execute("INSERT INTO eventos (id, nombre, fecha, modalidad) " +
                    "SELECT id, nombre, COALESCE(fecha, ''), COALESCE(modalidad, '') FROM eventos_old");
            statement.execute("INSERT INTO partidos (id, nombre, evento_id) " +
                    "SELECT p.id, p.nombre, p.evento_id FROM partidos_old p " +
                    "INNER JOIN eventos e ON e.id = p.evento_id");
            statement.execute("INSERT INTO gallos (id, nombre, peso, anillo, partido_id) " +
                    "SELECT g.id, g.nombre, g.peso, g.anillo, g.partido_id FROM gallos_old g " +
                    "INNER JOIN partidos p ON p.id = g.partido_id " +
                    "WHERE g.peso > 0");

            statement.execute("DROP TABLE gallos_old");
            statement.execute("DROP TABLE partidos_old");
            statement.execute("DROP TABLE eventos_old");

            connection.commit();
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            connection.setAutoCommit(previousAutoCommit);
        }
    }
}
