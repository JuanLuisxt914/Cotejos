package com.cotejador.app.data.sqlite;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteConnectionFactory {
    private static final String DEFAULT_DATABASE_URL = getDefaultDatabaseUrl();

    private final String databaseUrl;

    public SQLiteConnectionFactory() {
        this(DEFAULT_DATABASE_URL);
    }

    public SQLiteConnectionFactory(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(databaseUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private static String getDefaultDatabaseUrl() {
        String databasePath = getProjectRoot()
                .resolve("cotejador.db")
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace('\\', '/');
        return "jdbc:sqlite:" + databasePath;
    }

    private static Path getProjectRoot() {
        Path current = Paths.get("").toAbsolutePath();

        for (Path path = current; path != null; path = path.getParent()) {
            if (path.resolve("pom.xml").toFile().isFile()
                    && path.resolve("core").toFile().isDirectory()
                    && path.resolve("data-sqlite").toFile().isDirectory()
                    && path.resolve("desktop-javafx").toFile().isDirectory()) {
                return path;
            }
        }

        return current;
    }
}
