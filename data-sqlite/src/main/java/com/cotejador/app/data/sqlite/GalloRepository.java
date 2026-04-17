package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.Gallo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GalloRepository {
    private static final String INSERT_GALLO =
            "INSERT INTO gallos (nombre, peso, anillo, partido_id) VALUES (?, ?, ?, ?)";
    private static final String SELECT_GALLOS_BY_PARTIDO =
            "SELECT id, nombre, peso, anillo, partido_id FROM gallos WHERE partido_id = ? ORDER BY id";
    private static final String SELECT_GALLO_BY_ID =
            "SELECT id, nombre, peso, anillo, partido_id FROM gallos WHERE id = ?";
    private static final String UPDATE_GALLO =
            "UPDATE gallos SET nombre = ?, peso = ?, anillo = ? WHERE id = ?";
    private static final String DELETE_GALLO =
            "DELETE FROM gallos WHERE id = ?";

    private final SQLiteConnectionFactory connectionFactory;

    public GalloRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insertar(Gallo gallo) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_GALLO, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, gallo.getNombre());
            statement.setDouble(2, gallo.getPeso());
            statement.setString(3, gallo.getAnillo());
            statement.setLong(4, gallo.getPartidoId());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    gallo.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public List<Gallo> listarPorPartido(Long partidoId) throws SQLException {
        List<Gallo> gallos = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_GALLOS_BY_PARTIDO)) {
            statement.setLong(1, partidoId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    gallos.add(new Gallo(
                            resultSet.getLong("id"),
                            resultSet.getString("nombre"),
                            resultSet.getDouble("peso"),
                            resultSet.getString("anillo"),
                            resultSet.getLong("partido_id")
                    ));
                }
            }
        }

        return gallos;
    }

    public Gallo buscarPorId(Long id) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_GALLO_BY_ID)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Gallo(
                            resultSet.getLong("id"),
                            resultSet.getString("nombre"),
                            resultSet.getDouble("peso"),
                            resultSet.getString("anillo"),
                            resultSet.getLong("partido_id")
                    );
                }
            }
        }

        return null;
    }

    public void actualizar(Gallo gallo) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_GALLO)) {
            statement.setString(1, gallo.getNombre());
            statement.setDouble(2, gallo.getPeso());
            statement.setString(3, gallo.getAnillo());
            statement.setLong(4, gallo.getId());
            statement.executeUpdate();
        }
    }

    public void eliminar(Long id) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_GALLO)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
