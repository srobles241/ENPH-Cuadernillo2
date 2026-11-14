package enph;

import java.sql.*;
import java.time.LocalDate;

/** Un producto por hogar y fecha. El llamador debe bloquear el hogar en la misma transaccion. */
public final class GastoDiario {
    private GastoDiario() { }
    public static final class YaRegistrado extends SQLException {
        public final int gasto, visita;
        YaRegistrado(int gasto, int visita) {
            super("El producto ya está registrado para este hogar y fecha.");
            this.gasto=gasto; this.visita=visita;
        }
    }
    public static void exigirDisponible(Connection cn, int hogar, LocalDate fecha,
            int producto, int gastoActual, boolean conservaProducto) throws SQLException {
        // Permite corregir registros historicos aunque ya existieran duplicados.
        if (gastoActual != 0 && conservaProducto) return;
        try (PreparedStatement ps=cn.prepareStatement(
                "SELECT g.id_gasto,g.id_visita FROM gasto g JOIN visita v ON v.id_visita=g.id_visita "
                + "WHERE v.id_hogar=? AND TRUNC(v.fecha_visita)=? AND g.id_producto=? "
                + "AND g.id_gasto<>? ORDER BY g.id_gasto")) {
            ps.setInt(1,hogar); ps.setDate(2,Date.valueOf(fecha));
            ps.setInt(3,producto); ps.setInt(4,gastoActual);
            try (ResultSet rs=ps.executeQuery()) {
                if(rs.next())throw new YaRegistrado(rs.getInt(1),rs.getInt(2));
            }
        }
    }
}
