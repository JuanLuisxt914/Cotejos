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
            "INSERT INTO gallos (peso, anillo, partido_id, obligatorio) VALUES (?, ?, ?, ?)";
    private static final String SELECT_GALLOS_BY_PARTIDO =
            "SELECT id, peso, anillo, partido_id, obligatorio FROM gallos WHERE partido_id = ? ORDER BY id";
    private static final String SELECT_GALLO_BY_ID =
            "SELECT id, peso, anillo, partido_id, obligatorio FROM gallos WHERE id = ?";
    private static final String UPDATE_GALLO =
            "UPDATE gallos SET peso = ?, anillo = ?, obligatorio = ? WHERE id = ?";
    private static final String DELETE_GALLO =
            "DELETE FROM gallos WHERE id = ?";

    private final SQLiteConnectionFactory connectionFactory;

    public GalloRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insertar(Gallo gallo) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_GALLO, Statement.RETURN_GENERATED_KEYS)) {
            statement.setDouble(1, gallo.getPeso());
            statement.setString(2, gallo.getAnillo());
            statement.setLong(3, gallo.getPartidoId());
            statement.setInt(4, gallo.isObligatorio() ? 1 : 0);
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
                            resultSet.getDouble("peso"),
                            resultSet.getString("anillo"),
                            resultSet.getLong("partido_id"),
                            resultSet.getInt("obligatorio") != 0
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
                            resultSet.getDouble("peso"),
                            resultSet.getString("anillo"),
                            resultSet.getLong("partido_id"),
                            resultSet.getInt("obligatorio") != 0
                    );
                }
            }
        }

        return null;
    }

    public void actualizar(Gallo gallo) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_GALLO)) {
            statement.setDouble(1, gallo.getPeso());
            statement.setString(2, gallo.getAnillo());
            statement.setInt(3, gallo.isObligatorio() ? 1 : 0);
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
