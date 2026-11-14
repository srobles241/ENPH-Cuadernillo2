package enph;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.*;
import java.util.Date;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class RecuentistaFrame extends JFrame {

    private final JTable tabla = Estilo.tabla();
    private final JTextField txtCodigo = new JTextField();
    private final JTextField txtDireccion = new JTextField();
    private final JSpinner spnFecha =
            new JSpinner(new SpinnerDateModel());
    private final JComboBox<Item> cboEncuestador =
            new JComboBox<Item>();

    public RecuentistaFrame() {
        super("ENPH - Organización del recorrido");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(
            Estilo.encabezado(
                "Organización del recorrido",
                "Recuentista: " + Sesion.nombre
            ),
            BorderLayout.NORTH
        );

        spnFecha.setEditor(
            new JSpinner.DateEditor(spnFecha, "dd/MM/yyyy")
        );

        JPanel form = new JPanel();
        form.setBorder(BorderFactory.createTitledBorder(
            "Incorporar hogares y asignar responsables"
        ));

        Estilo.fila(form, 0, "Código del hogar (máx. 20)", txtCodigo);
        Estilo.fila(form, 1, "Dirección (máx. 150)", txtDireccion);
        Estilo.fila(form, 2, "Fecha de inicio del ciclo", spnFecha);
        Estilo.fila(form, 3, "Encuestador", cboEncuestador);

        JButton btnRegistrar = new JButton("Incorporar hogar al recorrido");
        JButton btnAsignar =
            new JButton("Asignar responsable de la encuesta");
        JButton btnActualizar = new JButton("Recargar información");
        JButton btnEliminar = new JButton("Eliminar hogar");
        JButton btnSalir = new JButton(Sesion.esAdmin() ? "Volver a administración" : "Cerrar sesión");
        JButton categorias = new JButton("Categorías de adquisición");

        JPanel botones =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        botones.add(btnRegistrar);
        botones.add(btnAsignar);
        botones.add(btnActualizar);

        JPanel secundarios =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        secundarios.add(btnEliminar);
        secundarios.add(btnSalir);
        secundarios.add(categorias);

        JPanel acciones =
            new JPanel(new java.awt.GridLayout(0, 1));
        acciones.add(botones);
        acciones.add(secundarios);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setBorder(
            BorderFactory.createEmptyBorder(8, 8, 0, 8)
        );
        norte.add(form, BorderLayout.CENTER);
        norte.add(acciones, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(norte, BorderLayout.NORTH);

        JPanel pTabla = new JPanel(new BorderLayout());
        pTabla.setBorder(
            BorderFactory.createEmptyBorder(0, 8, 8, 8)
        );
        pTabla.add(new JScrollPane(tabla), BorderLayout.CENTER);

        centro.add(pTabla, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        btnRegistrar.addActionListener(e -> registrar());
        btnAsignar.addActionListener(e -> asignar());
        btnActualizar.addActionListener(e -> cargar());
        btnEliminar.addActionListener(e -> eliminar());

        categorias.addActionListener(
            e -> new CategoriaFrame().setVisible(true)
        );

        btnSalir.addActionListener(e -> {
            if(Sesion.esAdmin()){dispose();return;}
            Sesion.cerrar();
            new LoginFrame().setVisible(true);
            dispose();
        });

        try {
            Conexion.cargarCombo(
                cboEncuestador,
                "SELECT id_encuestador, nombre_completo "
                + "FROM encuestador ORDER BY nombre_completo"
            );
            cboEncuestador.insertItemAt(
                new Item(0, "Sin asignar"), 0
            );
            cboEncuestador.setSelectedIndex(0);

        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }

        cargar();
        Estilo.agregarCerrar(this);
    }

    private void cargar() {
        String sql =
            "SELECT h.id_hogar, h.codigo_hogar, h.direccion, "
            + "h.estado, h.dia_ciclo_actual, h.fecha_inicio_ciclo, "
            + "NVL(e.nombre_completo, 'Sin asignar') AS encuestador "
            + "FROM hogar h "
            + "LEFT JOIN encuestador e "
            + "ON e.id_encuestador = h.id_encuestador_asignado "
            + "ORDER BY h.id_hogar";

        try {
            DefaultTableModel m = Conexion.consultar(sql);

            Estilo.mostrar(
                tabla, m,
                "ID", "Código", "Dirección", "Estado",
                "Día del ciclo", "Inicio del ciclo", "Encuestador"
            );

        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }

    private void registrar() {
        String codigo = txtCodigo.getText().trim();
        String direccion = txtDireccion.getText().trim();

        if (codigo.isEmpty() || direccion.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "El código y la dirección son obligatorios."
            );
            return;
        }

        if (codigo.length() > 20 || direccion.length() > 150) {
            JOptionPane.showMessageDialog(
                this,
                "El código admite hasta 20 caracteres "
                + "y la dirección hasta 150."
            );
            return;
        }

        Item enc = (Item) cboEncuestador.getSelectedItem();
        boolean asignado = enc != null && enc.id != 0;

        java.sql.Date fecha = new java.sql.Date(
            ((Date) spnFecha.getValue()).getTime()
        );

        try {
            Conexion.ejecutarAuditado("HOGAR", null, "CREAR",
                "INSERT INTO hogar "
                + "(id_hogar, id_recuentista, id_encuestador_asignado, "
                + "codigo_hogar, direccion, estado, "
                + "dia_ciclo_actual, fecha_inicio_ciclo) "
                + "VALUES (seq_hogar.NEXTVAL, ?, ?, ?, ?, ?, 1, ?)",
                Sesion.id,
                asignado ? (Object) Integer.valueOf(enc.id) : null,
                codigo,
                direccion,
                asignado ? "Asignado" : "Pendiente",
                fecha
            );

            txtCodigo.setText("");
            txtDireccion.setText("");
            cboEncuestador.setSelectedIndex(0);
            cargar();

        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1) {
                JOptionPane.showMessageDialog(
                    this,
                    "Ya existe un hogar con el código " + codigo + "."
                );
            } else {
                Conexion.mostrarError(this, ex);
            }
        }
    }

    private void asignar() {
        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            JOptionPane.showMessageDialog(
                this, "Selecciona un hogar de la lista."
            );
            return;
        }

        Item enc = (Item) cboEncuestador.getSelectedItem();

        if (enc == null || enc.id == 0) {
            JOptionPane.showMessageDialog(
                this, "Elige el encuestador que se asignará."
            );
            return;
        }

        int idHogar =
            ((Number) tabla.getValueAt(fila, 0)).intValue();

        try {
            int n = Conexion.ejecutarAuditado("HOGAR", idHogar, "EDITAR",
                "UPDATE hogar "
                + "SET id_encuestador_asignado = ?, estado = 'Asignado' "
                + "WHERE id_hogar = ? "
                + "AND estado IN ('Pendiente', 'Asignado') "
                + "AND NOT EXISTS ("
                + "SELECT 1 FROM caso c "
                + "WHERE c.id_hogar=hogar.id_hogar "
                + "AND c.estado_caso='Aprobado')",
                enc.id, idHogar
            );

            if (n == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Solo se puede asignar un hogar en estado "
                    + "\"Pendiente\" o \"Asignado\" "
                    + "y sin caso aprobado."
                );
            }

            cargar();

        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            JOptionPane.showMessageDialog(
                this, "Selecciona un hogar de la lista."
            );
            return;
        }

        if (!"Recuentista".equals(Sesion.rol)) {
            JOptionPane.showMessageDialog(
                this, "Esta opción es solo para recuentistas."
            );
            return;
        }

        int idHogar =
            ((Number) tabla.getValueAt(fila, 0)).intValue();

        String codigo =
            String.valueOf(tabla.getValueAt(fila, 1));

        int respuesta = JOptionPane.showConfirmDialog(
            this,
            "¿Eliminar el hogar " + codigo + "?\n"
            + "Solo se permite si lo registraste tú y no tiene "
            + "miembros, visitas ni casos.\n"
            + "Esta acción no se puede deshacer.",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection cn = Conexion.obtener()) {
            cn.setAutoCommit(false);

            try {
                // Comprueba la propiedad del hogar y bloquea su fila.
                try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT id_hogar FROM hogar "
                    + "WHERE id_hogar=? AND id_recuentista=? "
                    + "FOR UPDATE NOWAIT"
                )) {
                    ps.setInt(1, idHogar);
                    ps.setInt(2, Sesion.id);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                "El hogar ya no existe o fue registrado "
                                + "por otro recuentista."
                            );
                        }
                    }
                }

                // Evita agregar dependencias durante la eliminación.
                // Los bloqueos se liberan con commit o rollback.
                try (Statement st = cn.createStatement()) {
                    st.executeUpdate(
                        "LOCK TABLE miembro, visita, caso "
                        + "IN SHARE MODE NOWAIT"
                    );
                }

                String sql =
                    "DELETE FROM hogar "
                    + "WHERE id_hogar=? AND id_recuentista=? "
                    + "AND NOT EXISTS ("
                    + "SELECT 1 FROM miembro m "
                    + "WHERE m.id_hogar=hogar.id_hogar) "
                    + "AND NOT EXISTS ("
                    + "SELECT 1 FROM visita v "
                    + "WHERE v.id_hogar=hogar.id_hogar) "
                    + "AND NOT EXISTS ("
                    + "SELECT 1 FROM caso c "
                    + "WHERE c.id_hogar=hogar.id_hogar)";

                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setInt(1, idHogar);
                    ps.setInt(2, Sesion.id);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                            "No se puede eliminar: el hogar tiene "
                            + "miembros, visitas o casos asociados."
                        );
                    }
                }

                Auditoria.registrar(cn, "HOGAR", idHogar, "ELIMINAR");
                cn.commit();

            } catch (SQLException | RuntimeException ex) {
                try {
                    cn.rollback();
                } catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                }

                throw ex;
            }

        } catch (SQLException ex) {
            if (ex.getErrorCode() == 2292) {
                JOptionPane.showMessageDialog(
                    this,
                    "No se puede eliminar: existen registros "
                    + "relacionados con este hogar."
                );

            } else if (ex.getErrorCode() == 54) {
                JOptionPane.showMessageDialog(
                    this,
                    "Hay otra operación en curso. "
                    + "Intenta nuevamente en unos segundos."
                );

            } else {
                Conexion.mostrarError(this, ex);
            }

            return;
        }

        cargar();

        JOptionPane.showMessageDialog(
            this, "Hogar eliminado de la base de datos."
        );
    }
}
