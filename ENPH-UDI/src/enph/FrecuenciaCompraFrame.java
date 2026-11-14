package enph;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Indicador de frecuencia de compra: días distintos del ciclo (de 14) en que el hogar
 * registró la adquisición de un producto.
 */
public class FrecuenciaCompraFrame extends JFrame {

    private final int idHogar;
    private final JComboBox<Item> cboCategoria = new JComboBox<Item>();
    private final JTable tabla = Estilo.tabla();
    private final JLabel lblInfo = new JLabel(" ");

    // DOCUMENTACION: Construye los componentes de esta clase, configura su estado inicial y registra los eventos definidos en el constructor.
    public FrecuenciaCompraFrame(int idHogar, String codigo) {
        super("03. Frecuencia de compra - Hogar " + codigo);
        this.idHogar = idHogar;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(Estilo.encabezado("03. Reporte de frecuencia de compra",
                "Hogar " + codigo + "  |  Días con compra / 14 días del ciclo"), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnConsultar = new JButton("Ver resultados");
        filtro.add(new JLabel("Categoría"));
        filtro.add(cboCategoria);
        filtro.add(btnConsultar);
        centro.add(filtro, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        centro.add(lblInfo, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        btnConsultar.addActionListener(e -> cargar());

        try {
            Conexion.cargarCombo(cboCategoria, "SELECT id_categoria, nombre_categoria FROM categoria ORDER BY id_categoria");
            cboCategoria.insertItemAt(new Item(0, "Todas"), 0);
            cboCategoria.setSelectedIndex(0);
        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
        cargar();
        Estilo.agregarCerrar(this);
    }

    // DOCUMENTACION: Cuenta dias distintos con forma Compra por producto y categoria; calcula porcentaje sobre 14 dias y aplica el filtro de categoria.
    private void cargar() {
        Item cat = (Item) cboCategoria.getSelectedItem();
        int idCat = cat == null ? 0 : cat.id;
        String sql = "SELECT p.nombre_producto, c.nombre_categoria, "
                + "COUNT(DISTINCT v.numero_dia_ciclo) AS dias_con_compra, "
                + "ROUND(COUNT(DISTINCT v.numero_dia_ciclo) / 14 * 100, 1) AS frecuencia_pct "
                + "FROM visita v JOIN gasto g ON g.id_visita = v.id_visita "
                + "JOIN producto p ON p.id_producto = g.id_producto "
                + "JOIN categoria c ON c.id_categoria = p.id_categoria "
                + "WHERE v.id_hogar = ? AND v.numero_dia_ciclo BETWEEN 1 AND 14 AND g.forma_adquisicion = 'Compra' AND (? = 0 OR c.id_categoria = ?) "
                + "GROUP BY p.nombre_producto, c.nombre_categoria "
                + "ORDER BY dias_con_compra DESC, p.nombre_producto";
        try {
            DefaultTableModel m = Conexion.consultar(sql, idHogar, idCat, idCat);
            Estilo.mostrar(tabla, m, "Producto", "Categoría", "Días con compra (de 14)", "Frecuencia (%)");
            int dias = ((Number) Conexion.valor(
                    "SELECT COUNT(DISTINCT numero_dia_ciclo) FROM visita WHERE id_hogar = ?", idHogar)).intValue();
            lblInfo.setText("Días del ciclo con visita registrada: " + dias + " de 14");
        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }
}
