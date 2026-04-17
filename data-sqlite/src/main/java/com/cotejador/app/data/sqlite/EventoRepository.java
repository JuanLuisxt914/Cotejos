package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.Evento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EventoRepository {
    private static final String INSERT_EVENTO =
            "INSERT INTO eventos (nombre, fecha, modalidad) VALUES (?, ?, ?)";
    private static final String SELECT_EVENTOS =
            "SELECT id, nombre, fecha, modalidad FROM eventos ORDER BY id";
    private static final String UPDATE_EVENTO =
            "UPDATE eventos SET nombre = ?, fecha = ?, modalidad = ? WHERE id = ?";
    private static final String DELETE_EVENTO =
            "DELETE FROM eventos WHERE id = ?";

    private final SQLiteConnectionFactory connectionFactory;

    public EventoRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insertar(Evento evento) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_EVENTO, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, evento.getNombre());
            statement.setString(2, evento.getFecha());
            statement.setString(3, evento.getModalidad());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    evento.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public List<Evento> listarTodos() throws SQLException {
        List<Evento> eventos = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_EVENTOS);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                eventos.add(new Evento(
                        resultSet.getLong("id"),
                        resultSet.getString("nombre"),
                        resultSet.getString("fecha"),
                        resultSet.getString("modalidad")
                ));
            }
        }

        return eventos;
    }

    public void actualizar(Evento evento) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_EVENTO)) {
            statement.setString(1, evento.getNombre());
            statement.setString(2, evento.getFecha());
            statement.setString(3, evento.getModalidad());
            statement.setLong(4, evento.getId());
            statement.executeUpdate();
        }
    }

    public void eliminar(Long id) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_EVENTO)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
