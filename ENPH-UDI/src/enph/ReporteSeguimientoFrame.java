package enph;

import java.awt.BorderLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class ReporteSeguimientoFrame extends JFrame {
    static final String SQL_AVANCE = "SELECT d.dia, TRUNC(h.fecha_inicio_ciclo)+d.dia-1 AS fecha, "
        + "CASE WHEN COUNT(v.id_visita)=0 THEN 'Sin visita' "
        + "WHEN SUM(CASE WHEN v.estado_visita='Revisar' THEN 1 ELSE 0 END)>0 THEN 'Por revisar' "
        + "WHEN SUM(CASE WHEN v.estado_visita='Completa' THEN 1 ELSE 0 END)=COUNT(v.id_visita) "
        + "THEN 'Completa' ELSE 'Al dia' END AS estado "
        + "FROM hogar h CROSS JOIN (SELECT LEVEL dia FROM dual CONNECT BY LEVEL<=14) d "
        + "LEFT JOIN visita v ON v.id_hogar=h.id_hogar AND v.numero_dia_ciclo=d.dia "
        + "WHERE h.id_hogar=? AND h.id_encuestador_asignado=? "
        + "GROUP BY d.dia, TRUNC(h.fecha_inicio_ciclo)+d.dia-1 "
        + "ORDER BY d.dia ";

    static final String SQL_PENDIENTES = "SELECT c.id_caso, h.codigo_hogar, e.nombre_completo, c.estado_caso, "
        + "NVL(v.registrados,0) AS registrados, 14-NVL(v.registrados,0) AS faltantes, "
        + "NVL(v.por_revisar,0) AS por_revisar, "
        + "CASE WHEN NVL(v.registrados,0)=14 AND NVL(v.por_revisar,0)=0 "
        + "THEN 'Listo para revisar' ELSE 'Requiere atencion' END AS prioridad "
        + "FROM caso c JOIN hogar h ON h.id_hogar=c.id_hogar "
        + "JOIN encuestador e ON e.id_encuestador=c.id_encuestador "
        + "LEFT JOIN ( "
        + "SELECT id_hogar, COUNT(DISTINCT numero_dia_ciclo) AS registrados, "
        + "SUM(CASE WHEN estado_visita='Revisar' THEN 1 ELSE 0 END) AS por_revisar "
        + "FROM visita WHERE numero_dia_ciclo BETWEEN 1 AND 14 GROUP BY id_hogar "
        + ") v ON v.id_hogar=h.id_hogar "
        + "WHERE c.estado_caso IN ('Completo','Con alertas') "
        + "ORDER BY faltantes DESC, por_revisar DESC, c.id_caso ";

    static ReporteSeguimientoFrame avance(int hogar, String codigo) {
        return new ReporteSeguimientoFrame("Avance de visitas por hogar", "Hogar " + codigo,
                SQL_AVANCE, new Object[]{hogar, Sesion.id}, new String[]{"Día", "Fecha", "Estado de la visita"});
    }

    static ReporteSeguimientoFrame pendientes() {
        return new ReporteSeguimientoFrame("Casos pendientes de revisión", "Supervisor: " + Sesion.nombre,
                SQL_PENDIENTES, new Object[0], new String[]{"Caso", "Hogar", "Encuestador", "Estado",
                        "Días registrados", "Días faltantes", "Visitas por revisar", "Situación"});
    }

    private ReporteSeguimientoFrame(String titulo, String contexto, String sql, Object[] params, String[] columnas) {
        super(titulo);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(columnas.length > 3 ? 1000 : 680, 495);
        setLocationRelativeTo(null);
        add(Estilo.encabezado(titulo, contexto), BorderLayout.NORTH);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JTable tabla = Estilo.tabla();
        tabla.setAutoCreateRowSorter(true);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel acciones = new JPanel(new BorderLayout());
        JButton recargar = new JButton("Recargar información");
        JLabel estado = new JLabel();
        acciones.add(recargar, BorderLayout.WEST);
        acciones.add(estado, BorderLayout.EAST);
        panel.add(acciones, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
        Estilo.agregarCerrar(this);

        recargar.addActionListener(e -> {
            try {
                DefaultTableModel datos = Conexion.consultar(sql, params);
                Estilo.mostrar(tabla, datos, columnas);
                estado.setText(datos.getRowCount() == 0 ? "Sin resultados" : datos.getRowCount() + " filas");
            } catch (SQLException ex) {
                estado.setText("Error al consultar");
                Conexion.mostrarError(this, ex);
            }
        });
        recargar.doClick(0);
    }
}
