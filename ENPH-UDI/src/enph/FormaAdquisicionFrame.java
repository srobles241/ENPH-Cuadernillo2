package enph;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** CU 04: reporte compacto de gastos por forma de adquisición. */
public class FormaAdquisicionFrame extends JFrame {
    private final int idHogar;
    private final JTable tablaFormas = Estilo.tabla();
    private final JLabel lblTotal = new JLabel(" ");

    public FormaAdquisicionFrame(int idHogar, String codigo) {
        super("04. Gastos por forma de adquisición - " + codigo);
        this.idHogar = idHogar;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(Estilo.encabezado("04. Reporte de gastos por forma de adquisición",
            "Hogar " + codigo + " | Ciclo completo de 14 días"), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton recargar = new JButton("Recargar información");
        acciones.add(recargar);
        centro.add(acciones, BorderLayout.NORTH);
        centro.add(new JScrollPane(tablaFormas), BorderLayout.CENTER);
        centro.add(lblTotal, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        recargar.addActionListener(e -> cargarFormas());
        cargarFormas();
        Estilo.agregarCerrar(this);
    }

    private void cargarFormas() {
        String sql = "SELECT g.forma_adquisicion,COUNT(g.id_gasto),SUM(g.valor_pagado) "
            + "FROM gasto g JOIN visita v ON v.id_visita=g.id_visita "
            + "WHERE v.id_hogar=? AND v.numero_dia_ciclo BETWEEN 1 AND 14 "
            + "GROUP BY g.forma_adquisicion ORDER BY SUM(g.valor_pagado) DESC";
        try {
            DefaultTableModel m = Conexion.consultar(sql, idHogar);
            Estilo.mostrar(tablaFormas, m, "Forma de adquisición", "N.º de registros", "Valor pagado total");
            BigDecimal total = BigDecimal.ZERO;
            int registros = 0;
            for(int i=0;i<m.getRowCount();i++) {
                registros += ((Number)m.getValueAt(i,1)).intValue();
                total = total.add(new BigDecimal(m.getValueAt(i,2).toString()));
            }
            lblTotal.setText(String.format("Valor pagado: $ %,.2f | Registros: %d", total, registros));
        } catch(SQLException ex) { Conexion.mostrarError(this,ex); }
    }
}
