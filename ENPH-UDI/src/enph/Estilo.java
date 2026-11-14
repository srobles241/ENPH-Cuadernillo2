package enph;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Utilidades de apariencia comunes a todas las pantallas. */
public class Estilo {

    public static final Color PRIMARIO = new Color(0x1F4E79);

    // DOCUMENTACION: Establece la configuracion regional es-CO y el aspecto Nimbus, fuentes y colores compartidos.
    public static void iniciar() {
        java.util.Locale.setDefault(new java.util.Locale("es", "CO"));
        try { javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); }
        catch (Exception ignored) { }
        javax.swing.UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 13));
        javax.swing.UIManager.put("control",new Color(0xF4F6F9));
        javax.swing.UIManager.put("nimbusBase",PRIMARIO);
        javax.swing.UIManager.put("nimbusSelectionBackground",PRIMARIO);
    }

    // DOCUMENTACION: Crea el panel azul del titulo y agrega subtitulo cuando existe.
    public static JPanel encabezado(String titulo, String subtitulo) {
        JPanel p = new JPanel(new GridLayout(0, 1));
        p.setBackground(PRIMARIO);
        p.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel t = new JLabel(titulo);
        t.setFont(new Font("SansSerif", Font.BOLD, 20));
        t.setForeground(Color.WHITE);
        p.add(t);
        if (subtitulo != null && !subtitulo.isEmpty()) {
            JLabel s = new JLabel(subtitulo);
            s.setForeground(new Color(0xD6E4F0));
            p.add(s);
        }
        return p;
    }

    // DOCUMENTACION: Crea una tabla de seleccion unica con altura y apariencia compartidas.
    public static JTable tabla() {
        JTable t = new JTable();
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.setRowHeight(32);
        t.setShowGrid(false);
        t.setIntercellSpacing(new java.awt.Dimension(0, 1));
        t.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,13));
        return t;
    }

    /** Asigna el modelo a la tabla y, si coincide el número de columnas, pone encabezados en español. */
    // DOCUMENTACION: Asigna el modelo, aplica titulos si coinciden las columnas y desactiva su reordenamiento.
    public static void mostrar(JTable tabla, DefaultTableModel modelo, String... titulos) {
        if (titulos.length == modelo.getColumnCount()) {
            modelo.setColumnIdentifiers(titulos);
        }
        tabla.setModel(modelo);
        tabla.getTableHeader().setReorderingAllowed(false);
    }

    /** Agrega una fila "etiqueta + componente" a un panel con GridBagLayout. */
    // DOCUMENTACION: Coloca una etiqueta y su componente en dos columnas mediante GridBagLayout.
    public static void fila(JPanel p, int fila, String etiqueta, JComponent c) {
        if (!(p.getLayout() instanceof GridBagLayout)) {
            p.setLayout(new GridBagLayout());
        }
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 7, 7, 7);
        g.gridy = fila;
        g.gridx = 0;
        g.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(etiqueta), g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(c, g);
    }

    // DOCUMENTACION: Distribuye los botones en una cuadricula de cuatro columnas con margenes.
    public static JPanel botones(JButton... bs) {
        JPanel p = new JPanel(new java.awt.GridLayout(0, bs.length > 4 ? 3 : 4, 8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
        for (JButton b : bs) {
            p.add(b);
        }
        return p;
    }

    /** Conserva el contenido inferior y ofrece el mismo cierre que la X de la ventana. */
    public static void agregarCerrar(javax.swing.JFrame ventana) {
        java.awt.Container contenido = ventana.getContentPane();
        java.awt.BorderLayout layout = (java.awt.BorderLayout) contenido.getLayout();
        java.awt.Component anterior = layout.getLayoutComponent(java.awt.BorderLayout.SOUTH);
        javax.swing.JPanel pie = new javax.swing.JPanel(new java.awt.BorderLayout());
        if (anterior != null) { contenido.remove(anterior); pie.add(anterior,java.awt.BorderLayout.CENTER); }
        javax.swing.JPanel acciones = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        javax.swing.JButton cerrar = new javax.swing.JButton("Cerrar");
        cerrar.addActionListener(e -> ventana.dispatchEvent(new java.awt.event.WindowEvent(
            ventana, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        acciones.add(cerrar); pie.add(acciones,java.awt.BorderLayout.SOUTH);
        contenido.add(pie,java.awt.BorderLayout.SOUTH);
    }
}
