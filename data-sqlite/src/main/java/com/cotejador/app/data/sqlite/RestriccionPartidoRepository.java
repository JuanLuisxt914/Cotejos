package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.RestriccionPartido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RestriccionPartidoRepository {
    private static final String INSERT_RESTRICCION =
            "INSERT INTO restricciones_partidos " +
                    "(evento_id, partido_origen_id, partido_destino_id, tipo) VALUES (?, ?, ?, ?)";
    private static final String SELECT_RESTRICCIONES_BY_EVENTO =
            "SELECT id, evento_id, partido_origen_id, partido_destino_id, tipo " +
                    "FROM restricciones_partidos WHERE evento_id = ? ORDER BY id";
    private static final String DELETE_RESTRICCION =
            "DELETE FROM restricciones_partidos WHERE id = ?";

    private final SQLiteConnectionFactory connectionFactory;

    public RestriccionPartidoRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insertar(RestriccionPartido restriccion) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_RESTRICCION, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, restriccion.getEventoId());
            statement.setLong(2, restriccion.getPartidoOrigenId());
            statement.setLong(3, restriccion.getPartidoDestinoId());
            statement.setString(4, restriccion.getTipo());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    restriccion.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public List<RestriccionPartido> listarPorEvento(Long eventoId) throws SQLException {
        List<RestriccionPartido> restricciones = new ArrayList<RestriccionPartido>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_RESTRICCIONES_BY_EVENTO)) {
            statement.setLong(1, eventoId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    restricciones.add(new RestriccionPartido(
                            resultSet.getLong("id"),
                            resultSet.getLong("evento_id"),
                            resultSet.getLong("partido_origen_id"),
                            resultSet.getLong("partido_destino_id"),
                            resultSet.getString("tipo")
                    ));
                }
            }
        }

        return restricciones;
    }

    public void eliminar(Long id) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_RESTRICCION)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
