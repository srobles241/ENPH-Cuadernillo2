package enph;

import java.awt.BorderLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Pantalla principal del encuestador: hogares asignados y accesos a cada función. */
public class EncuestadorFrame extends JFrame {

    private final JTable tabla = Estilo.tabla();
    private final javax.swing.JLabel lblAyuda = new javax.swing.JLabel("Selecciona un hogar para registrar sus adquisiciones o enviarlo a revisión.");

    // DOCUMENTACION: Construye los componentes de esta clase, configura su estado inicial y registra los eventos definidos en el constructor.
    public EncuestadorFrame() {
        super("ENPH - Hogares asignados");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Estilo.encabezado("Hogares asignados", "Encuestador: " + Sesion.nombre), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        lblAyuda.setBorder(BorderFactory.createEmptyBorder(8,8,12,8));
        centro.add(lblAyuda,BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        JButton btnGasto = new JButton("01. Gasto diario del hogar");
        JButton btnResumen = new JButton("02. Resumen de gastos por hogar");
        JButton btnFrecuencia = new JButton("03. Frecuencia de compra");
        JButton btnForma = new JButton("04. Gastos por forma de adquisición");
        JButton btnAvance = new JButton("Avance de visitas por hogar");
        JButton btnCerrarCaso = new JButton("Enviar hogar a revisión");
        JButton btnActualizar = new JButton("Recargar información");
        JButton btnSalir = new JButton(Sesion.esAdmin() ? "Volver a administración" : "Cerrar sesión");
        add(Estilo.botones(btnGasto, btnResumen, btnFrecuencia, btnForma, btnAvance, btnCerrarCaso, btnActualizar, btnSalir),
                BorderLayout.SOUTH);

        btnAvance.setEnabled(false);
        btnGasto.setEnabled(false); btnCerrarCaso.setEnabled(false);
        btnResumen.setEnabled(false); btnFrecuencia.setEnabled(false); btnForma.setEnabled(false);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            int row=tabla.getSelectedRow(); boolean elegido=row>=0;
            btnAvance.setEnabled(elegido);
            btnResumen.setEnabled(elegido); btnFrecuencia.setEnabled(elegido); btnForma.setEnabled(elegido);
            boolean asignado=elegido && "Asignado".equals(String.valueOf(tabla.getValueAt(row,3)));
            btnGasto.setEnabled(asignado);
            // Permitir consultar la validación; no enviar un caso incompleto.
            btnCerrarCaso.setEnabled(asignado);
            if(elegido) {
                int dias=((Number)tabla.getValueAt(row,5)).intValue();
                lblAyuda.setText(asignado ? (dias==14 ? "Hogar con 14 días: puedes crear el caso y enviarlo al supervisor." : "Hogar seleccionado: "+dias+" de 14 días. Completa los días restantes antes de enviar el caso.") : "Este hogar está cerrado o aprobado. Puedes consultar sus registros.");
            } else if(tabla.getRowCount()>0) lblAyuda.setText("Selecciona un hogar para registrar gastos o crear su caso.");
        });
        btnCerrarCaso.setToolTipText("Crea el caso del hogar seleccionado y lo envía al supervisor cuando tiene sus 14 días registrados.");
        btnGasto.addActionListener(e -> abrirRegistrarGasto());
        btnResumen.addActionListener(e -> {
            if (hayHogar()) {
                new ResumenHogarFrame(idHogar(), codigo()).setVisible(true);
            }
        });
        btnFrecuencia.addActionListener(e -> {
            if (hayHogar()) {
                new FrecuenciaCompraFrame(idHogar(), codigo()).setVisible(true);
            }
        });
        btnForma.addActionListener(e -> {
            if (hayHogar()) {
                new FormaAdquisicionFrame(idHogar(), codigo()).setVisible(true);
            }
        });
        btnAvance.addActionListener(e -> {
            if (hayHogar()) ReporteSeguimientoFrame.avance(idHogar(), codigo()).setVisible(true);
        });
        btnCerrarCaso.addActionListener(e -> cerrarCaso());
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

    // DOCUMENTACION: Consulta los hogares asignados al usuario y cuenta sus dias con visita; muestra Aprobado si existe un caso aprobado.
    private void cargar() {
        String sql = "SELECT h.id_hogar, h.codigo_hogar, h.direccion, CASE WHEN EXISTS (SELECT 1 FROM caso c WHERE c.id_hogar=h.id_hogar AND c.estado_caso='Aprobado') THEN 'Aprobado' ELSE h.estado END AS estado, h.dia_ciclo_actual, "
                + "(SELECT COUNT(DISTINCT v.numero_dia_ciclo) FROM visita v WHERE v.id_hogar = h.id_hogar) AS dias_registrados "
                + "FROM hogar h WHERE h.id_encuestador_asignado = ? ORDER BY h.codigo_hogar";
        try {
            DefaultTableModel m = Conexion.consultar(sql, Sesion.id);
            Estilo.mostrar(tabla, m, "ID", "Código", "Dirección", "Estado", "Día del ciclo", "Días con visita (de 14)");
            lblAyuda.setText(m.getRowCount()==0 ? "<html>No tienes hogares asignados. Pide al recuentista que te asigne un hogar.<br>Después vuelve a ingresar con tu usuario de encuestador.</html>" : "Selecciona un hogar para registrar gastos o crear su caso.");
        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }

    // DOCUMENTACION: Comprueba que haya una fila seleccionada y avisa si falta elegir un hogar.
    private boolean hayHogar() {
        if (tabla.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un hogar de la lista.");
            return false;
        }
        return true;
    }

    // DOCUMENTACION: Obtiene el identificador del hogar de la fila seleccionada.
    private int idHogar() {
        return ((Number) tabla.getValueAt(tabla.getSelectedRow(), 0)).intValue();
    }

    // DOCUMENTACION: Obtiene el codigo visible del hogar seleccionado.
    private String codigo() {
        return String.valueOf(tabla.getValueAt(tabla.getSelectedRow(), 1));
    }

    // DOCUMENTACION: Comprueba la seleccion y el estado Asignado antes de abrir el registro de gastos.
    private void abrirRegistrarGasto() {
        if (!hayHogar()) {
            return;
        }
        int fila = tabla.getSelectedRow();
        String estado = String.valueOf(tabla.getValueAt(fila, 3));
        if (!"Asignado".equals(estado)) {
            JOptionPane.showMessageDialog(this,
                    "Solo se pueden registrar gastos en hogares con estado \"Asignado\".\nEstado actual: " + estado);
            return;
        }
        int dia = ((Number) tabla.getValueAt(fila, 4)).intValue();
        new RegistrarGastoFrame(idHogar(), codigo(), dia).setVisible(true);
    }

    /** Audita los 14 días antes de permitir el cierre definitivo del caso. */
    // DOCUMENTACION: Comprueba los 14 dias y las visitas pendientes, solicita confirmacion y delega el cierre a CasoServicio.
    private void cerrarCaso() {
        if (!hayHogar()) {
            return;
        }
        int fila = tabla.getSelectedRow();
        int idHogar = idHogar();
        String estado = String.valueOf(tabla.getValueAt(fila, 3));
        if (!"Asignado".equals(estado)) {
            JOptionPane.showMessageDialog(this, "Solo se puede cerrar un hogar con estado \"Asignado\".");
            return;
        }
        try {
            int dias = ((Number) Conexion.valor(
                    "SELECT COUNT(DISTINCT numero_dia_ciclo) FROM visita WHERE id_hogar = ?", idHogar)).intValue();
            int porRevisar = ((Number) Conexion.valor(
                    "SELECT COUNT(*) FROM visita WHERE id_hogar = ? AND estado_visita = 'Revisar'", idHogar)).intValue();

            if (dias < 14) {
                JOptionPane.showMessageDialog(this, "Todavía no se puede crear el caso para supervisor: faltan " + (14 - dias)
                        + " día(s) del ciclo por registrar (llevas " + dias + " de 14).",
                        "Caso incompleto", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (porRevisar > 0) {
                JOptionPane.showMessageDialog(this, "Todavía no se puede enviar el caso: hay " + porRevisar
                        + " visita(s) en estado \"Revisar\" con datos por aclarar.",
                        "Caso con pendientes", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "Los 14 días están registrados. ¿Crear el caso del hogar "
                    + codigo() + " y enviarlo a revisión del supervisor?", "Enviar hogar a revisión",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            CasoServicio.cerrar(idHogar);
            Conexion.info(this, "Caso creado y enviado. El supervisor ya puede consultarlo y aprobarlo.");
            cargar();
        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }
}
