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
                    "modalidad TEXT NOT NULL, " +
                    "gallos_por_partido INTEGER NOT NULL DEFAULT 0, " +
                    "gallos_obligatorios INTEGER NOT NULL DEFAULT 0, " +
                    "ultima_ronda_solo_obligatorios INTEGER NOT NULL DEFAULT 0, " +
                    "modo_cotejo TEXT NOT NULL DEFAULT 'ALEATORIO', " +
                    "excluir_obligatorios_del_cotejo INTEGER NOT NULL DEFAULT 0" +
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
                    "obligatorio INTEGER NOT NULL DEFAULT 0, " +
                    "partido_id INTEGER NOT NULL, " +
                    "FOREIGN KEY (partido_id) REFERENCES partidos(id) ON DELETE CASCADE" +
                    ")";
    private static final String CREATE_RESTRICCIONES_PARTIDOS_TABLE =
            "CREATE TABLE IF NOT EXISTS restricciones_partidos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "evento_id INTEGER NOT NULL, " +
                    "partido_origen_id INTEGER NOT NULL, " +
                    "partido_destino_id INTEGER NOT NULL, " +
                    "tipo TEXT NOT NULL, " +
                    "CHECK (partido_origen_id <> partido_destino_id), " +
                    "FOREIGN KEY (evento_id) REFERENCES eventos(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (partido_origen_id) REFERENCES partidos(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (partido_destino_id) REFERENCES partidos(id) ON DELETE CASCADE" +
                    ")";
    private static final String CREATE_RESTRICCIONES_PARTIDOS_UNIQUE_INDEX =
            "CREATE UNIQUE INDEX IF NOT EXISTS ux_restricciones_partidos_logica " +
                    "ON restricciones_partidos (" +
                    "evento_id, " +
                    "tipo, " +
                    "CASE WHEN partido_origen_id < partido_destino_id THEN partido_origen_id ELSE partido_destino_id END, " +
                    "CASE WHEN partido_origen_id < partido_destino_id THEN partido_destino_id ELSE partido_origen_id END" +
                    ")";
    private static final String CREATE_COTEJOS_TABLE =
            "CREATE TABLE IF NOT EXISTS cotejos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "evento_id INTEGER NOT NULL, " +
                    "tolerancia_gramos REAL NOT NULL, " +
                    "fecha_generacion TEXT NOT NULL, " +
                    "FOREIGN KEY (evento_id) REFERENCES eventos(id) ON DELETE CASCADE" +
                    ")";
    private static final String CREATE_PELEAS_GENERADAS_TABLE =
            "CREATE TABLE IF NOT EXISTS peleas_generadas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "cotejo_id INTEGER NOT NULL, " +
                    "orden INTEGER NOT NULL, " +
                    "gallo_1_id INTEGER NOT NULL, " +
                    "gallo_2_id INTEGER NOT NULL, " +
                    "diferencia_peso REAL NOT NULL, " +
                    "FOREIGN KEY (cotejo_id) REFERENCES cotejos(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (gallo_1_id) REFERENCES gallos(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (gallo_2_id) REFERENCES gallos(id) ON DELETE CASCADE" +
                    ")";
    private static final String CREATE_GALLOS_SIN_PELEA_TABLE =
            "CREATE TABLE IF NOT EXISTS gallos_sin_pelea (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "cotejo_id INTEGER NOT NULL, " +
                    "gallo_id INTEGER NOT NULL, " +
                    "FOREIGN KEY (cotejo_id) REFERENCES cotejos(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (gallo_id) REFERENCES gallos(id) ON DELETE CASCADE" +
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
            statement.execute(CREATE_RESTRICCIONES_PARTIDOS_TABLE);
            statement.execute(CREATE_RESTRICCIONES_PARTIDOS_UNIQUE_INDEX);
            createCotejoTables(statement);
            ensureEventoColumns(connection);
            if (schemaNeedsMigration(connection)) {
                migrateSchema(connection);
            }
        }
    }

    private boolean schemaNeedsMigration(Connection connection) throws SQLException {
        return !hasForeignKeys(connection, "partidos") || !hasForeignKeys(connection, "gallos");
    }

    private void ensureEventoColumns(Connection connection) throws SQLException {
        if (!hasColumn(connection, "eventos", "gallos_por_partido")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE eventos ADD COLUMN gallos_por_partido INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(connection, "eventos", "gallos_obligatorios")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE eventos ADD COLUMN gallos_obligatorios INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(connection, "eventos", "ultima_ronda_solo_obligatorios")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE eventos ADD COLUMN ultima_ronda_solo_obligatorios INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(connection, "eventos", "modo_cotejo")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE eventos ADD COLUMN modo_cotejo TEXT NOT NULL DEFAULT 'ALEATORIO'");
            }
        }
        if (!hasColumn(connection, "eventos", "excluir_obligatorios_del_cotejo")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE eventos ADD COLUMN excluir_obligatorios_del_cotejo INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(connection, "gallos", "obligatorio")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE gallos ADD COLUMN obligatorio INTEGER NOT NULL DEFAULT 0");
            }
        }
    }

    private boolean hasForeignKeys(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_list(" + tableName + ")")) {
            return resultSet.next();
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
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
            statement.execute(CREATE_RESTRICCIONES_PARTIDOS_TABLE);
            statement.execute(CREATE_RESTRICCIONES_PARTIDOS_UNIQUE_INDEX);
            createCotejoTables(statement);

            statement.execute("INSERT INTO eventos (id, nombre, fecha, modalidad, gallos_por_partido, gallos_obligatorios, ultima_ronda_solo_obligatorios, modo_cotejo, excluir_obligatorios_del_cotejo) " +
                    "SELECT id, nombre, COALESCE(fecha, ''), COALESCE(modalidad, ''), 0, 0, 0, 'ALEATORIO', 0 FROM eventos_old");
            statement.execute("INSERT INTO partidos (id, nombre, evento_id) " +
                    "SELECT p.id, p.nombre, p.evento_id FROM partidos_old p " +
                    "INNER JOIN eventos e ON e.id = p.evento_id");
            statement.execute("INSERT INTO gallos (id, nombre, peso, anillo, obligatorio, partido_id) " +
                    "SELECT g.id, g.nombre, g.peso, g.anillo, 0, g.partido_id FROM gallos_old g " +
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

    private void createCotejoTables(Statement statement) throws SQLException {
        statement.execute(CREATE_COTEJOS_TABLE);
        statement.execute(CREATE_PELEAS_GENERADAS_TABLE);
        statement.execute(CREATE_GALLOS_SIN_PELEA_TABLE);
    }
}
