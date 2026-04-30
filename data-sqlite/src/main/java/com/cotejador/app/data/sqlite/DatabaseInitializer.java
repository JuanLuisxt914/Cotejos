package com.cotejador.app.data.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

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
                    "preferencia_orden TEXT NOT NULL DEFAULT 'NORMAL', " +
                    "FOREIGN KEY (evento_id) REFERENCES eventos(id) ON DELETE CASCADE" +
                    ")";
    private static final String CREATE_GALLOS_TABLE =
            "CREATE TABLE IF NOT EXISTS gallos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "peso REAL NOT NULL CHECK (peso > 0), " +
                    "anillo TEXT, " +
                    "obligatorio INTEGER NOT NULL DEFAULT 0, " +
                    "orden_registro INTEGER NOT NULL DEFAULT 0, " +
                    "preferencia_orden TEXT NOT NULL DEFAULT 'SIN_PREFERENCIA', " +
                    "ronda_preferida INTEGER, " +
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
                    "ronda INTEGER NOT NULL DEFAULT 1, " +
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
                    "ronda INTEGER NOT NULL DEFAULT 1, " +
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
            repairBrokenCotejoSchema(connection, statement);
            ensureCotejoDetailColumns(connection);
        }
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
        if (!hasColumn(connection, "gallos", "orden_registro")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE gallos ADD COLUMN orden_registro INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(connection, "partidos", "preferencia_orden")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE partidos ADD COLUMN preferencia_orden TEXT NOT NULL DEFAULT 'NORMAL'");
            }
        }
        if (!hasColumn(connection, "gallos", "preferencia_orden")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE gallos ADD COLUMN preferencia_orden TEXT NOT NULL DEFAULT 'SIN_PREFERENCIA'");
            }
        }
        if (!hasColumn(connection, "gallos", "ronda_preferida")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE gallos ADD COLUMN ronda_preferida INTEGER");
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "UPDATE gallos SET orden_registro = (" +
                            "SELECT COUNT(*) FROM gallos g2 " +
                            "WHERE g2.partido_id = gallos.partido_id AND g2.id <= gallos.id" +
                            ") WHERE orden_registro <= 0");
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

    private void ensureCotejoDetailColumns(Connection connection) throws SQLException {
        if (!hasColumn(connection, "peleas_generadas", "ronda")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE peleas_generadas ADD COLUMN ronda INTEGER NOT NULL DEFAULT 1");
            }
        }
        if (!hasColumn(connection, "gallos_sin_pelea", "ronda")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE gallos_sin_pelea ADD COLUMN ronda INTEGER NOT NULL DEFAULT 1");
            }
        }
    }

    private void repairBrokenCotejoSchema(Connection connection, Statement statement) throws SQLException {
        List<String> tablesToRepair = Arrays.asList("cotejos", "peleas_generadas", "gallos_sin_pelea", "restricciones_partidos");
        boolean requiresRepair = false;
        for (String table : tablesToRepair) {
            if (tableSqlReferencesOldTables(connection, table)) {
                requiresRepair = true;
                break;
            }
        }

        if (!requiresRepair) {
            return;
        }

        statement.execute("DROP TABLE IF EXISTS gallos_sin_pelea");
        statement.execute("DROP TABLE IF EXISTS peleas_generadas");
        statement.execute("DROP TABLE IF EXISTS cotejos");
        statement.execute("DROP TABLE IF EXISTS restricciones_partidos");

        statement.execute(CREATE_RESTRICCIONES_PARTIDOS_TABLE);
        statement.execute(CREATE_RESTRICCIONES_PARTIDOS_UNIQUE_INDEX);
        createCotejoTables(statement);
    }

    private boolean tableSqlReferencesOldTables(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = '" + tableName + "'")) {
            if (resultSet.next()) {
                String sql = resultSet.getString(1);
                return sql != null && (sql.contains("eventos_old") || sql.contains("partidos_old") || sql.contains("gallos_old"));
            }
        }
        return false;
    }

    private void createCotejoTables(Statement statement) throws SQLException {
        statement.execute(CREATE_COTEJOS_TABLE);
        statement.execute(CREATE_PELEAS_GENERADAS_TABLE);
        statement.execute(CREATE_GALLOS_SIN_PELEA_TABLE);
    }
}
