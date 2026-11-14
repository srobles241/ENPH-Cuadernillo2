package enph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Guarda la auditoría en la misma transacción que el cambio. */
final class Auditoria {
    private Auditoria() {}

    static void registrar(Connection c, String tabla, Object id, String accion) throws SQLException {
        registrar(c, tabla, id, accion, Sesion.marcaAuditoria());
    }

    static void registrar(Connection c, String tabla, Object id, String accion, String contexto) throws SQLException {
        if (contexto == null) return;
        try (PreparedStatement p = c.prepareStatement("BEGIN ENPH_REGISTRAR_AUD(?,?,?,?); END;")) {
            p.setString(1, tabla);
            p.setString(2, String.valueOf(id));
            p.setString(3, accion);
            p.setString(4, contexto);
            p.execute();
        }
    }

    static void alta(Connection c, String tabla) throws SQLException {
        if (!Sesion.esAdmin()) return;
        // Los llamadores pasan nombres de tabla constantes del código.
        try (PreparedStatement p = c.prepareStatement("SELECT SEQ_" + tabla + ".CURRVAL FROM DUAL");
             ResultSet r = p.executeQuery()) {
            if (r.next()) registrar(c, tabla, r.getLong(1), "CREAR");
        }
    }

    static void visitasDelHogar(Connection c, int hogar) throws SQLException {
        if (!Sesion.esAdmin()) return;
        try (PreparedStatement p = c.prepareStatement("SELECT id_visita FROM visita WHERE id_hogar=?")) {
            p.setInt(1, hogar);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) registrar(c, "VISITA", r.getInt(1), "EDITAR");
            }
        }
    }
}
