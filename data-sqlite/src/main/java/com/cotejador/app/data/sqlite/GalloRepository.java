package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.PreferenciaOrdenGallo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GalloRepository {
    private static final String INSERT_GALLO =
            "INSERT INTO gallos (peso, anillo, partido_id, obligatorio, orden_registro, preferencia_orden, ronda_preferida) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_GALLOS_BY_PARTIDO =
            "SELECT id, peso, anillo, partido_id, obligatorio, orden_registro, preferencia_orden, ronda_preferida " +
                    "FROM gallos WHERE partido_id = ? ORDER BY orden_registro, id";
    private static final String SELECT_GALLO_BY_ID =
            "SELECT id, peso, anillo, partido_id, obligatorio, orden_registro, preferencia_orden, ronda_preferida FROM gallos WHERE id = ?";
    private static final String UPDATE_GALLO =
            "UPDATE gallos SET peso = ?, anillo = ?, obligatorio = ?, orden_registro = ?, preferencia_orden = ?, ronda_preferida = ? WHERE id = ?";
    private static final String DELETE_GALLO =
            "DELETE FROM gallos WHERE id = ?";
    private static final String SELECT_NEXT_ORDEN =
            "SELECT COALESCE(MAX(orden_registro), 0) + 1 FROM gallos WHERE partido_id = ?";
    private static final String SELECT_PARTIDO_BY_GALLO =
            "SELECT partido_id FROM gallos WHERE id = ?";
    private static final String RENUMBER_GALLOS_BY_PARTIDO =
            "UPDATE gallos SET orden_registro = (" +
                    "SELECT COUNT(*) FROM gallos g2 " +
                    "WHERE g2.partido_id = gallos.partido_id " +
                    "AND g2.id <= gallos.id" +
                    ") WHERE partido_id = ?";

    private final SQLiteConnectionFactory connectionFactory;

    public GalloRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insertar(Gallo gallo) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_GALLO, Statement.RETURN_GENERATED_KEYS)) {
            int ordenRegistro = gallo.getOrdenRegistro() > 0
                    ? gallo.getOrdenRegistro()
                    : nextOrdenRegistro(connection, gallo.getPartidoId());
            statement.setDouble(1, gallo.getPeso());
            statement.setString(2, gallo.getAnillo());
            statement.setLong(3, gallo.getPartidoId());
            statement.setInt(4, gallo.isObligatorio() ? 1 : 0);
            statement.setInt(5, ordenRegistro);
            statement.setString(6, gallo.getPreferenciaOrden().name());
            if (gallo.getRondaPreferida() == null) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, gallo.getRondaPreferida());
            }
            statement.executeUpdate();
            gallo.setOrdenRegistro(ordenRegistro);

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
                            resultSet.getInt("obligatorio") != 0,
                            resultSet.getInt("orden_registro"),
                            PreferenciaOrdenGallo.fromDb(resultSet.getString("preferencia_orden")),
                            getNullableInteger(resultSet, "ronda_preferida")
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
                            resultSet.getInt("obligatorio") != 0,
                            resultSet.getInt("orden_registro"),
                            PreferenciaOrdenGallo.fromDb(resultSet.getString("preferencia_orden")),
                            getNullableInteger(resultSet, "ronda_preferida")
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
            statement.setInt(4, gallo.getOrdenRegistro());
            statement.setString(5, gallo.getPreferenciaOrden().name());
            if (gallo.getRondaPreferida() == null) {
                statement.setNull(6, java.sql.Types.INTEGER);
            } else {
                statement.setInt(6, gallo.getRondaPreferida());
            }
            statement.setLong(7, gallo.getId());
            statement.executeUpdate();
        }
    }

    public void eliminar(Long id) throws SQLException {
        try (Connection connection = connectionFactory.getConnection()) {
            Long partidoId = buscarPartidoId(connection, id);
            try (PreparedStatement statement = connection.prepareStatement(DELETE_GALLO)) {
                statement.setLong(1, id);
                statement.executeUpdate();
            }
            if (partidoId != null) {
                renumerarGallosPorPartido(connection, partidoId);
            }
        }
    }

    private int nextOrdenRegistro(Connection connection, Long partidoId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_NEXT_ORDEN)) {
            statement.setLong(1, partidoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 1;
            }
        }
    }

    private Long buscarPartidoId(Connection connection, Long galloId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_PARTIDO_BY_GALLO)) {
            statement.setLong(1, galloId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("partido_id") : null;
            }
        }
    }

    private void renumerarGallosPorPartido(Connection connection, Long partidoId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(RENUMBER_GALLOS_BY_PARTIDO)) {
            statement.setLong(1, partidoId);
            statement.executeUpdate();
        }
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }
}
