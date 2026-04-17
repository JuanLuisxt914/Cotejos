package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.CotejoGuardado;
import com.cotejador.app.core.model.GalloSinPeleaGuardado;
import com.cotejador.app.core.model.PeleaGuardada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CotejoRepository {
    private static final String INSERT_COTEJO =
            "INSERT INTO cotejos (evento_id, tolerancia_gramos, fecha_generacion) VALUES (?, ?, ?)";
    private static final String INSERT_PELEA =
            "INSERT INTO peleas_generadas " +
                    "(cotejo_id, orden, gallo_1_id, gallo_2_id, diferencia_peso) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_GALLO_SIN_PELEA =
            "INSERT INTO gallos_sin_pelea (cotejo_id, gallo_id) VALUES (?, ?)";
    private static final String SELECT_COTEJOS_BY_EVENTO =
            "SELECT id, evento_id, tolerancia_gramos, fecha_generacion " +
                    "FROM cotejos WHERE evento_id = ? ORDER BY id DESC";
    private static final String SELECT_COTEJO_BY_ID =
            "SELECT id, evento_id, tolerancia_gramos, fecha_generacion FROM cotejos WHERE id = ?";
    private static final String SELECT_PELEAS_BY_COTEJO =
            "SELECT id, cotejo_id, orden, gallo_1_id, gallo_2_id, diferencia_peso " +
                    "FROM peleas_generadas WHERE cotejo_id = ? ORDER BY orden";
    private static final String SELECT_GALLOS_SIN_PELEA_BY_COTEJO =
            "SELECT id, cotejo_id, gallo_id FROM gallos_sin_pelea WHERE cotejo_id = ? ORDER BY id";
    private static final String DELETE_COTEJO =
            "DELETE FROM cotejos WHERE id = ?";

    private final SQLiteConnectionFactory connectionFactory;

    public CotejoRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void guardar(CotejoGuardado cotejo) throws SQLException {
        try (Connection connection = connectionFactory.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                insertarCotejo(connection, cotejo);
                insertarPeleas(connection, cotejo);
                insertarGallosSinPelea(connection, cotejo);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public List<CotejoGuardado> listarPorEvento(Long eventoId) throws SQLException {
        List<CotejoGuardado> cotejos = new ArrayList<CotejoGuardado>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_COTEJOS_BY_EVENTO)) {
            statement.setLong(1, eventoId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    cotejos.add(readCotejo(resultSet));
                }
            }
        }

        return cotejos;
    }

    public CotejoGuardado cargarDetalle(Long cotejoId) throws SQLException {
        CotejoGuardado cotejo = null;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_COTEJO_BY_ID)) {
            statement.setLong(1, cotejoId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    cotejo = readCotejo(resultSet);
                }
            }
        }

        if (cotejo != null) {
            cotejo.setPeleas(listarPeleas(cotejoId));
            cotejo.setGallosSinPelea(listarGallosSinPelea(cotejoId));
        }

        return cotejo;
    }

    public void eliminar(Long id) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_COTEJO)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private void insertarCotejo(Connection connection, CotejoGuardado cotejo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_COTEJO, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, cotejo.getEventoId());
            statement.setDouble(2, cotejo.getToleranciaGramos());
            statement.setString(3, cotejo.getFechaGeneracion());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    cotejo.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    private void insertarPeleas(Connection connection, CotejoGuardado cotejo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_PELEA)) {
            for (PeleaGuardada pelea : cotejo.getPeleas()) {
                statement.setLong(1, cotejo.getId());
                statement.setInt(2, pelea.getOrden());
                statement.setLong(3, pelea.getGallo1Id());
                statement.setLong(4, pelea.getGallo2Id());
                statement.setDouble(5, pelea.getDiferenciaPeso());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertarGallosSinPelea(Connection connection, CotejoGuardado cotejo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_GALLO_SIN_PELEA)) {
            for (GalloSinPeleaGuardado gallo : cotejo.getGallosSinPelea()) {
                statement.setLong(1, cotejo.getId());
                statement.setLong(2, gallo.getGalloId());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<PeleaGuardada> listarPeleas(Long cotejoId) throws SQLException {
        List<PeleaGuardada> peleas = new ArrayList<PeleaGuardada>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PELEAS_BY_COTEJO)) {
            statement.setLong(1, cotejoId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    peleas.add(new PeleaGuardada(
                            resultSet.getLong("id"),
                            resultSet.getLong("cotejo_id"),
                            resultSet.getInt("orden"),
                            resultSet.getLong("gallo_1_id"),
                            resultSet.getLong("gallo_2_id"),
                            resultSet.getDouble("diferencia_peso")
                    ));
                }
            }
        }

        return peleas;
    }

    private List<GalloSinPeleaGuardado> listarGallosSinPelea(Long cotejoId) throws SQLException {
        List<GalloSinPeleaGuardado> gallos = new ArrayList<GalloSinPeleaGuardado>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_GALLOS_SIN_PELEA_BY_COTEJO)) {
            statement.setLong(1, cotejoId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    gallos.add(new GalloSinPeleaGuardado(
                            resultSet.getLong("id"),
                            resultSet.getLong("cotejo_id"),
                            resultSet.getLong("gallo_id")
                    ));
                }
            }
        }

        return gallos;
    }

    private CotejoGuardado readCotejo(ResultSet resultSet) throws SQLException {
        return new CotejoGuardado(
                resultSet.getLong("id"),
                resultSet.getLong("evento_id"),
                resultSet.getDouble("tolerancia_gramos"),
                resultSet.getString("fecha_generacion")
        );
    }
}
