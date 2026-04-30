package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.Partido;
import com.cotejador.app.core.model.PreferenciaOrdenPartido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PartidoRepository {
    private static final String INSERT_PARTIDO =
            "INSERT INTO partidos (nombre, evento_id, preferencia_orden) VALUES (?, ?, ?)";
    private static final String SELECT_PARTIDOS_BY_EVENTO =
            "SELECT id, nombre, evento_id, preferencia_orden FROM partidos WHERE evento_id = ? ORDER BY id";
    private static final String SELECT_PARTIDO_BY_ID =
            "SELECT id, nombre, evento_id, preferencia_orden FROM partidos WHERE id = ?";
    private static final String UPDATE_PARTIDO =
            "UPDATE partidos SET nombre = ?, preferencia_orden = ? WHERE id = ?";
    private static final String DELETE_PARTIDO =
            "DELETE FROM partidos WHERE id = ?";

    private final SQLiteConnectionFactory connectionFactory;

    public PartidoRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insertar(Partido partido) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_PARTIDO, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, partido.getNombre());
            statement.setLong(2, partido.getEventoId());
            statement.setString(3, partido.getPreferenciaOrden().name());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    partido.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public List<Partido> listarPorEvento(Long eventoId) throws SQLException {
        List<Partido> partidos = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PARTIDOS_BY_EVENTO)) {
            statement.setLong(1, eventoId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    partidos.add(new Partido(
                            resultSet.getLong("id"),
                            resultSet.getString("nombre"),
                            resultSet.getLong("evento_id"),
                            PreferenciaOrdenPartido.fromDb(resultSet.getString("preferencia_orden"))
                    ));
                }
            }
        }

        return partidos;
    }

    public Partido buscarPorId(Long id) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PARTIDO_BY_ID)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Partido(
                            resultSet.getLong("id"),
                            resultSet.getString("nombre"),
                            resultSet.getLong("evento_id"),
                            PreferenciaOrdenPartido.fromDb(resultSet.getString("preferencia_orden"))
                    );
                }
            }
        }

        return null;
    }

    public void actualizar(Partido partido) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
            PreparedStatement statement = connection.prepareStatement(UPDATE_PARTIDO)) {
            statement.setString(1, partido.getNombre());
            statement.setString(2, partido.getPreferenciaOrden().name());
            statement.setLong(3, partido.getId());
            statement.executeUpdate();
        }
    }

    public void eliminar(Long id) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_PARTIDO)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
