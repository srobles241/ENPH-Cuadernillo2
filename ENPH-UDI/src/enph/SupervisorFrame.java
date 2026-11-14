package enph;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Consulta del supervisor: revisa, aprueba o devuelve los casos cerrados por los encuestadores. */
public class SupervisorFrame extends JFrame {

    private final JTable tabla = Estilo.tabla();
    private final JComboBox<String> cboEstado = new JComboBox<String>(
            new String[]{"Todos", "Completo", "Con alertas", "Aprobado", "Devuelto"});

    // DOCUMENTACION: Construye los componentes de esta clase, configura su estado inicial y registra los eventos definidos en el constructor.
    public SupervisorFrame() {
        super("ENPH - Revisión del operativo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 540);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(Estilo.encabezado("Revisión del operativo", "Supervisor: " + Sesion.nombre), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtro.add(new JLabel("Estado del caso"));
        filtro.add(cboEstado);
        centro.add(filtro, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        JButton btnResumen = new JButton("02. Resumen de gastos por hogar");
        JButton btnFrecuencia = new JButton("03. Frecuencia de compra");
        JButton btnForma = new JButton("04. Gastos por forma de adquisición");
        JButton btnPendientes = new JButton("Casos pendientes de revisión");
        JButton btnAprobar = new JButton("Aprobar caso");
        JButton btnDevolver = new JButton("Devolver caso");
        JButton btnActualizar = new JButton("Recargar información");
        JButton btnSalir = new JButton(Sesion.esAdmin() ? "Volver a administración" : "Cerrar sesión");
        add(Estilo.botones(btnResumen, btnFrecuencia, btnForma, btnPendientes, btnAprobar, btnDevolver, btnActualizar, btnSalir),
                BorderLayout.SOUTH);

        btnAprobar.setEnabled(false); btnDevolver.setEnabled(false);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            int fila=tabla.getSelectedRow();
            boolean revisable=fila>=0 && ("Completo".equals(String.valueOf(tabla.getValueAt(fila,5))) || "Con alertas".equals(String.valueOf(tabla.getValueAt(fila,5))));
            btnAprobar.setEnabled(revisable); btnDevolver.setEnabled(revisable);
        });
        tabla.setDefaultRenderer(Object.class,new javax.swing.table.DefaultTableCellRenderer() {
            // DOCUMENTACION: Colorea en verde las filas de casos aprobados cuando no estan seleccionadas.
            public java.awt.Component getTableCellRendererComponent(JTable t,Object v,boolean elegido,boolean foco,int f,int col) {
                java.awt.Component c=super.getTableCellRendererComponent(t,v,elegido,foco,f,col);
                if(!elegido) c.setForeground("Aprobado".equals(String.valueOf(t.getValueAt(f,5)))?new java.awt.Color(0x187547):java.awt.Color.BLACK);
                return c;
            }
        });
        cboEstado.addActionListener(e -> cargar());
        btnResumen.addActionListener(e -> {
            if (hayCaso()) {
                new ResumenHogarFrame(idHogar(), codigo()).setVisible(true);
            }
        });
        btnFrecuencia.addActionListener(e -> {
            if (hayCaso()) {
                new FrecuenciaCompraFrame(idHogar(), codigo()).setVisible(true);
            }
        });
        btnForma.addActionListener(e -> {
            if (hayCaso()) {
                new FormaAdquisicionFrame(idHogar(), codigo()).setVisible(true);
            }
        });
        btnPendientes.addActionListener(e -> ReporteSeguimientoFrame.pendientes().setVisible(true));
        btnAprobar.addActionListener(e -> aprobar());
        btnDevolver.addActionListener(e -> devolver());
        btnActualizar.addActionListener(e -> cargar());
        btnSalir.addActionListener(e -> {
            if(Sesion.esAdmin()){dispose();return;}
            Sesion.cerrar();
            new LoginFrame().setVisible(true);
            dispose();
        });
        cargar();
        Estilo.agregarCerrar(this);
    }

    // DOCUMENTACION: Consulta casos segun el filtro de estado y muestra hogar, encuestador, dias registrados, gasto total y supervisor.
    private void cargar() {
        String estado = (String) cboEstado.getSelectedItem();
        String sql = "SELECT c.id_caso, h.id_hogar, h.codigo_hogar, h.direccion, e.nombre_completo AS encuestador, "
                + "c.estado_caso, "
                + "(SELECT COUNT(DISTINCT v.numero_dia_ciclo) FROM visita v WHERE v.id_hogar = h.id_hogar) AS dias_registrados, "
                + "enph3_total_hogar(h.id_hogar) AS total_gastado, "
                + "s.nombre_completo AS supervisor "
                + "FROM caso c JOIN hogar h ON h.id_hogar = c.id_hogar "
                + "JOIN encuestador e ON e.id_encuestador = c.id_encuestador "
                + "LEFT JOIN supervisor s ON s.id_supervisor = c.id_supervisor_aprobador "
                + "WHERE (? = 'Todos' OR c.estado_caso = ?) ORDER BY c.id_caso";
        try {
            DefaultTableModel m = Conexion.consultar(sql, estado, estado);
            Estilo.mostrar(tabla, m, "ID caso", "ID hogar", "Código", "Dirección", "Encuestador",
                    "Estado del caso", "Días registrados", "Total gastado", "Supervisor");
        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }

    // DOCUMENTACION: Verifica la seleccion de un caso antes de operar.
    private boolean hayCaso() {
        if (tabla.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un caso de la lista.");
            return false;
        }
        return true;
    }

    // DOCUMENTACION: Obtiene el identificador del caso seleccionado.
    private int idCaso() {
        return ((Number) tabla.getValueAt(tabla.getSelectedRow(), 0)).intValue();
    }

    // DOCUMENTACION: Obtiene el identificador del hogar asociado al caso seleccionado.
    private int idHogar() {
        return ((Number) tabla.getValueAt(tabla.getSelectedRow(), 1)).intValue();
    }

    // DOCUMENTACION: Obtiene el codigo del hogar de la fila seleccionada.
    private String codigo() {
        return String.valueOf(tabla.getValueAt(tabla.getSelectedRow(), 2));
    }

    // DOCUMENTACION: Obtiene el estado visible del caso seleccionado.
    private String estadoCaso() {
        return String.valueOf(tabla.getValueAt(tabla.getSelectedRow(), 5));
    }

    // DOCUMENTACION: Valida el estado, solicita confirmacion y delega la aprobacion; despues muestra el resultado aprobado.
    private void aprobar() {
        if (!hayCaso()) {
            return;
        }
        String estado = estadoCaso();
        if (!"Completo".equals(estado) && !"Con alertas".equals(estado)) {
            JOptionPane.showMessageDialog(this, "Solo se pueden aprobar casos en estado \"Completo\" o \"Con alertas\".\n"
                    + "Estado actual: " + estado);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "¿Aprobar el caso del hogar " + codigo() + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int caso=idCaso(), hogar=idHogar();
            CasoServicio.aprobar(caso,hogar);
            // Mantener visible el resultado aunque el filtro anterior fuera Completo.
            cboEstado.setSelectedItem("Aprobado");
            cargar();
            for(int i=0;i<tabla.getRowCount();i++)if(((Number)tabla.getValueAt(i,0)).intValue()==caso)tabla.setRowSelectionInterval(i,i);
            Conexion.info(this,"Aprobación guardada en Oracle. El caso permanecerá aprobado al volver a ingresar.");
        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }

    /** Devuelve el caso al encuestador y reabre el hogar para que pueda corregir. */
    // DOCUMENTACION: Valida el estado, solicita confirmacion y delega la devolucion para reabrir el hogar.
    private void devolver() {
        if (!hayCaso()) {
            return;
        }
        String estado = estadoCaso();
        if (!"Completo".equals(estado) && !"Con alertas".equals(estado)) {
            JOptionPane.showMessageDialog(this, "Solo se pueden devolver casos en estado \"Completo\" o \"Con alertas\".\n"
                    + "Estado actual: " + estado);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "¿Devolver el caso del hogar " + codigo() + " al encuestador?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            CasoServicio.devolver(idCaso(),idHogar());
            cargar();
        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }
}
