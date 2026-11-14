package enph;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** CU 02: resumen del ciclo completo, con accesos a los reportes complementarios. */
public class ResumenHogarFrame extends JFrame {
    private final int idHogar;
    private final String codigo;
    private final JTable tabla = Estilo.tabla();
    private final JLabel lblTotal = new JLabel(" ");

    public ResumenHogarFrame(int idHogar, String codigo) {
        super("02. Resumen de gastos por hogar - " + codigo);
        this.idHogar = idHogar;
        this.codigo = codigo;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(850, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(Estilo.encabezado("02. Resumen de gastos por hogar",
            "Hogar " + codigo + " | Ciclo completo de 14 días"), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JButton recargar = new JButton("Recargar información");
        JPanel opciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        opciones.add(new JLabel("Todos los días del ciclo"));
        opciones.add(recargar);
        centro.add(opciones, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        centro.add(lblTotal, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        JButton frecuencia = new JButton("03. Frecuencia de compra");
        JButton modalidades = new JButton("04. Gastos por forma de adquisición");
        JPanel enlaces = new JPanel(new java.awt.GridLayout(1, 2, 8, 8));
        enlaces.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        enlaces.add(frecuencia); enlaces.add(modalidades);
        add(enlaces, BorderLayout.SOUTH);
        frecuencia.setToolTipText("Reporte de frecuencia de compra del hogar seleccionado");
        modalidades.setToolTipText("Reporte de gastos por forma de adquisición del hogar seleccionado");
        recargar.addActionListener(e -> cargar());
        frecuencia.addActionListener(e -> new FrecuenciaCompraFrame(this.idHogar, this.codigo).setVisible(true));
        modalidades.addActionListener(e -> new FormaAdquisicionFrame(this.idHogar, this.codigo).setVisible(true));
        cargar();
        Estilo.agregarCerrar(this);
    }

    private void cargar() {
        String sql = "SELECT c.nombre_categoria, COUNT(g.id_gasto), SUM(g.valor_pagado) "
            + "FROM visita v JOIN gasto g ON g.id_visita=v.id_visita "
            + "JOIN producto p ON p.id_producto=g.id_producto "
            + "JOIN categoria c ON c.id_categoria=p.id_categoria "
            + "WHERE v.id_hogar=? AND v.numero_dia_ciclo BETWEEN 1 AND 14 "
            + "GROUP BY c.nombre_categoria ORDER BY SUM(g.valor_pagado) DESC";
        try {
            DefaultTableModel m = Conexion.consultar(sql, idHogar);
            Estilo.mostrar(tabla, m, "Categoría", "N.º de registros", "Total gastado");
            BigDecimal total = BigDecimal.ZERO;
            int registros = 0;
            for (int i=0; i<m.getRowCount(); i++) {
                registros += ((Number)m.getValueAt(i, 1)).intValue();
                total = total.add(new BigDecimal(m.getValueAt(i, 2).toString()));
            }
            lblTotal.setText(String.format("Total gastado: $ %,.2f | Registros: %d", total, registros));
        } catch (SQLException ex) { Conexion.mostrarError(this, ex); }
    }
}

