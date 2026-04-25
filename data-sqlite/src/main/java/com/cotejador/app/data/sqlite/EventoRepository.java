package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.ModoCotejo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EventoRepository {
    private static final String INSERT_EVENTO =
            "INSERT INTO eventos (nombre, fecha, modalidad, gallos_por_partido, gallos_obligatorios, ultima_ronda_solo_obligatorios, modo_cotejo, excluir_obligatorios_del_cotejo) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_EVENTOS =
            "SELECT id, nombre, fecha, modalidad, gallos_por_partido, gallos_obligatorios, ultima_ronda_solo_obligatorios, modo_cotejo, excluir_obligatorios_del_cotejo " +
                    "FROM eventos ORDER BY id";
    private static final String SELECT_EVENTO_BY_ID =
            "SELECT id, nombre, fecha, modalidad, gallos_por_partido, gallos_obligatorios, ultima_ronda_solo_obligatorios, modo_cotejo, excluir_obligatorios_del_cotejo " +
                    "FROM eventos WHERE id = ?";
    private static final String UPDATE_EVENTO =
            "UPDATE eventos SET nombre = ?, fecha = ?, modalidad = ?, gallos_por_partido = ?, gallos_obligatorios = ?, " +
                    "ultima_ronda_solo_obligatorios = ?, modo_cotejo = ?, excluir_obligatorios_del_cotejo = ? WHERE id = ?";
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
            statement.setInt(4, evento.getGallosPorPartido());
            statement.setInt(5, evento.getGallosObligatorios());
            statement.setInt(6, evento.isUltimaRondaSoloObligatorios() ? 1 : 0);
            statement.setString(7, evento.getModoCotejo().name());
            statement.setInt(8, evento.isExcluirObligatoriosDelCotejo() ? 1 : 0);
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
                        resultSet.getString("modalidad"),
                        resultSet.getInt("gallos_por_partido"),
                        resultSet.getInt("gallos_obligatorios"),
                        resultSet.getInt("ultima_ronda_solo_obligatorios") != 0,
                        parseModoCotejo(resultSet.getString("modo_cotejo")),
                        resultSet.getInt("excluir_obligatorios_del_cotejo") != 0
                ));
            }
        }

        return eventos;
    }

    public Evento buscarPorId(Long id) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_EVENTO_BY_ID)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Evento(
                            resultSet.getLong("id"),
                            resultSet.getString("nombre"),
                            resultSet.getString("fecha"),
                            resultSet.getString("modalidad"),
                            resultSet.getInt("gallos_por_partido"),
                            resultSet.getInt("gallos_obligatorios"),
                            resultSet.getInt("ultima_ronda_solo_obligatorios") != 0,
                            parseModoCotejo(resultSet.getString("modo_cotejo")),
                            resultSet.getInt("excluir_obligatorios_del_cotejo") != 0
                    );
                }
            }
        }

        return null;
    }

    public void actualizar(Evento evento) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
            PreparedStatement statement = connection.prepareStatement(UPDATE_EVENTO)) {
            statement.setString(1, evento.getNombre());
            statement.setString(2, evento.getFecha());
            statement.setString(3, evento.getModalidad());
            statement.setInt(4, evento.getGallosPorPartido());
            statement.setInt(5, evento.getGallosObligatorios());
            statement.setInt(6, evento.isUltimaRondaSoloObligatorios() ? 1 : 0);
            statement.setString(7, evento.getModoCotejo().name());
            statement.setInt(8, evento.isExcluirObligatoriosDelCotejo() ? 1 : 0);
            statement.setLong(9, evento.getId());
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

    private ModoCotejo parseModoCotejo(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ModoCotejo.ALEATORIO;
        }

        try {
            return ModoCotejo.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ModoCotejo.ALEATORIO;
        }
    }
}
