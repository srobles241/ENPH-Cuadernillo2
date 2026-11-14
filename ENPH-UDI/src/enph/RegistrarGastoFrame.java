package enph;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class RegistrarGastoFrame extends JFrame {

    private final int idHogar;
    private boolean cargando;
    private LocalDate inicioCiclo;

    private final Map<Integer, LocalDate> fechasVisita =
            new HashMap<>();

    private final JComboBox<Item> cboVisita = new JComboBox<>();
    private final SelectorFecha selectorFecha = new SelectorFecha();
    private final JLabel lblPeriodo = new JLabel(" ");

    private final JComboBox<Item> cboCategoria = new JComboBox<>();
    private final JComboBox<Item> cboProducto = new JComboBox<>();
    private final JComboBox<String> cboUnidad = UnidadesMedida.combo();

    private final JTextField txtCantidad = new JTextField();
    private final JTextField txtValor = new JTextField();

    private final JComboBox<String> cboForma = new JComboBox<>(
        new String[]{
            "Compra", "Regalo", "Trueque",
            "Pago en especie", "Produccion propia"
        }
    );

    private final JTable tabla = Estilo.tabla();
    private final JLabel lblTotal =
            new JLabel("Total de la jornada: $ 0,00");

    private final JButton btnGuardar =
            new JButton("Registrar adquisición");
    private final JButton btnCancelar =
            new JButton("Cancelar corrección");
    private final JButton btnNuevaVisita =
            new JButton("Abrir jornada de la fecha");

    private Datos original;

    private static final class Datos {
        int gasto;
        int visita;
        int producto;
        Integer miembro;
        BigDecimal cantidad;
        BigDecimal valor;
        String forma;
        String unidad;
    }

    public RegistrarGastoFrame(
            int idHogar, String codigoHogar, int diaCiclo) {

        super("01. Gasto diario del hogar - " + codigoHogar);
        this.idHogar = idHogar;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, Math.min(800, java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(
            Estilo.encabezado(
                "01. Gasto diario del hogar",
                "Hogar " + codigoHogar
                    + " | Encuestador: " + Sesion.nombre
            ),
            BorderLayout.NORTH
        );

        JPanel form = new JPanel();
        form.setBorder(
            BorderFactory.createEmptyBorder(8, 12, 4, 12)
        );

        JPanel fechas = new JPanel(new BorderLayout());
        JPanel linea =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        linea.add(selectorFecha);
        linea.add(btnNuevaVisita);

        fechas.add(linea, BorderLayout.CENTER);
        fechas.add(lblPeriodo, BorderLayout.SOUTH);

        JPanel productos = new JPanel(new BorderLayout(6, 0));
        JButton btnOtro = new JButton("Otro producto…");

        productos.add(cboProducto, BorderLayout.CENTER);
        productos.add(btnOtro, BorderLayout.EAST);

        cboUnidad.setToolTipText(
            "Selecciona una unidad de la lista. "
            + "La cantidad debe expresarse en esa unidad."
        );

        Estilo.fila(form, 0, "Fecha del gasto", fechas);
        Estilo.fila(form, 1, "Visitas registradas", cboVisita);
        Estilo.fila(form, 2, "Categoria", cboCategoria);
        Estilo.fila(form, 3, "Producto", productos);
        Estilo.fila(form, 4, "Unidad de medida", cboUnidad);
        Estilo.fila(form, 5, "Cantidad", txtCantidad);
        Estilo.fila(form, 6, "Valor pagado ($)", txtValor);
        Estilo.fila(form, 7, "Forma de adquisición", cboForma);

        JButton btnEditar =
                new JButton("Corregir adquisición");

        JPanel botones =
                new JPanel(new FlowLayout(FlowLayout.RIGHT));

        botones.add(btnEditar);
        botones.add(btnCancelar);
        botones.add(btnGuardar);


        add(new JScrollPane(form), BorderLayout.CENTER);

        JPanel sur = new JPanel(new BorderLayout());
        sur.setBorder(
            BorderFactory.createEmptyBorder(0, 12, 8, 12)
        );

        JPanel cabeceraLista = new JPanel(new BorderLayout());
        cabeceraLista.add(botones, BorderLayout.NORTH);
        cabeceraLista.add(new JLabel("Adquisiciones de la visita seleccionada"), BorderLayout.SOUTH);
        sur.add(cabeceraLista, BorderLayout.NORTH);

        JScrollPane lista = new JScrollPane(tabla);
        lista.setPreferredSize(
            new java.awt.Dimension(800, 100)
        );

        sur.add(lista, BorderLayout.CENTER);
        sur.add(lblTotal, BorderLayout.SOUTH);
        add(sur, BorderLayout.SOUTH);

        btnCancelar.setEnabled(false);

        cboCategoria.addActionListener(e -> {
            if (!cargando) {
                cargarProductos(0);
            }
        });

        cboProducto.addActionListener(e -> {
            if (!cargando) {
                Item p = (Item) cboProducto.getSelectedItem();
                UnidadesMedida.seleccionar(cboUnidad, p == null ? null : p.extra);
            }
        });

        cboForma.addActionListener(e -> actualizarPrecio());

        cboVisita.addActionListener(e -> {
            if (!cargando) {
                Item v = (Item) cboVisita.getSelectedItem();

                if (v != null) {
                    cargando = true;
                    try {
                        selectorFecha.setFecha(
                            fechasVisita.get(v.id)
                        );
                    } finally {
                        cargando = false;
                    }
                }

                cargarGastos();
            }
        });

        selectorFecha.addChangeListener(e -> {
            if (!cargando) {
                seleccionarFecha();
            }
        });

        btnNuevaVisita.addActionListener(e -> crearVisita());
        btnOtro.addActionListener(e -> otroProducto());
        btnEditar.addActionListener(e -> editar());
        btnCancelar.addActionListener(e -> terminarEdicion());
        btnGuardar.addActionListener(e -> guardar());

        cboForma.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    JList<?> lista, Object valor, int indice, boolean seleccionado, boolean foco) {
                String texto = valor == null ? "Selecciona una forma de adquisición" : valor.toString();
                if ("Produccion propia".equals(texto)) texto = "Producción propia";
                return super.getListCellRendererComponent(lista, texto, indice, seleccionado, foco);
            }
        });
        cboForma.setSelectedIndex(-1);
        cargarCatalogos();
        cargarVisitas(diaCiclo);
        Estilo.agregarCerrar(this);
    }

    private void cargarCatalogos() {
        cargando = true;

        try {
            Object fecha = Conexion.valor(
                "SELECT fecha_inicio_ciclo FROM hogar "
                + "WHERE id_hogar=?",
                idHogar
            );

            if (!(fecha instanceof java.util.Date)) {
                throw new SQLException(
                    "El hogar no tiene fecha de inicio."
                );
            }

            inicioCiclo = new java.sql.Date(
                ((java.util.Date) fecha).getTime()
            ).toLocalDate();

            DateTimeFormatter f =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy");

            lblPeriodo.setText(
                "Ciclo: " + inicioCiclo.format(f)
                + " a " + inicioCiclo.plusDays(13).format(f)
            );

            selectorFecha.setFecha(inicioCiclo);

            Conexion.cargarCombo(
                cboCategoria,
                "SELECT id_categoria,nombre_categoria "
                + "FROM categoria ORDER BY nombre_categoria"
            );

        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        } finally {
            cargando = false;
        }

        cargarProductos(0);
    }

    private void cargarProductos(int seleccionar) {
        Item cat = (Item) cboCategoria.getSelectedItem();
        cargando = true;

        try {
            cboProducto.removeAllItems();

            if (cat != null) {
                Conexion.cargarCombo(
                    cboProducto,
                    "SELECT id_producto,nombre_producto,unidad_medida "
                    + "FROM producto WHERE id_categoria=? "
                    + "ORDER BY nombre_producto",
                    cat.id
                );
            }

            if (seleccionar != 0) {
                seleccionarItem(cboProducto, seleccionar);
            }

            Item p = (Item) cboProducto.getSelectedItem();
            UnidadesMedida.seleccionar(cboUnidad, p == null ? null : p.extra);

        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        } finally {
            cargando = false;
        }
    }

    private boolean seleccionarItem(
            JComboBox<Item> combo, int id) {

        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id == id) {
                combo.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    private void cargarVisitas(int diaSeleccionar) {
        cargando = true;

        try {
            DefaultTableModel m = Conexion.consultar(
                "SELECT id_visita,numero_dia_ciclo,"
                + "fecha_visita,estado_visita "
                + "FROM visita WHERE id_hogar=? "
                + "ORDER BY numero_dia_ciclo",
                idHogar
            );

            cboVisita.removeAllItems();
            fechasVisita.clear();

            int seleccion = -1;
            DateTimeFormatter f =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (int i = 0; i < m.getRowCount(); i++) {
                int id =
                    ((Number) m.getValueAt(i, 0)).intValue();
                int dia =
                    ((Number) m.getValueAt(i, 1)).intValue();

                fechasVisita.put(
                    id,
                    LocalDate.parse(
                        String.valueOf(m.getValueAt(i, 2)), f
                    )
                );

                cboVisita.addItem(
                    new Item(
                        id,
                        "Dia " + dia + " - " + m.getValueAt(i, 2)
                        + " (" + m.getValueAt(i, 3) + ")",
                        String.valueOf(dia)
                    )
                );

                if (dia == diaSeleccionar) {
                    seleccion = i;
                }
            }

            if (cboVisita.getItemCount() > 0) {
                cboVisita.setSelectedIndex(
                    seleccion >= 0
                        ? seleccion
                        : cboVisita.getItemCount() - 1
                );

                Item actual =
                    (Item) cboVisita.getSelectedItem();

                selectorFecha.setFecha(
                    fechasVisita.get(actual.id)
                );
            }

        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        } finally {
            cargando = false;
        }

        cargarGastos();
    }

    private void seleccionarFecha() {
        if (inicioCiclo == null) {
            return;
        }

        cargando = true;

        try {
            LocalDate fecha = selectorFecha.getFecha();
            cboVisita.setSelectedIndex(-1);

            for (int i = 0; i < cboVisita.getItemCount(); i++) {
                Item visita = cboVisita.getItemAt(i);

                if (fecha.equals(fechasVisita.get(visita.id))) {
                    cboVisita.setSelectedIndex(i);
                    break;
                }
            }

        } finally {
            cargando = false;
        }

        cargarGastos();
    }

    private void crearVisita() {
        try {
            LocalDate fecha = selectorFecha.getFecha();

            VisitaServicio.obtenerOCrear(idHogar, fecha);

            cargarVisitas(
                VisitaServicio.diaDelCiclo(inicioCiclo, fecha)
            );

        } catch (SQLException | IllegalArgumentException ex) {
            Conexion.mostrarError(this, ex);
        }
    }

    private void cargarGastos() {
        Item v = (Item) cboVisita.getSelectedItem();

        if (v == null) {
            tabla.setModel(new DefaultTableModel());
            lblTotal.setText("Total de la jornada: $ 0,00");
            return;
        }

        try {
            DefaultTableModel m = Conexion.consultar(
                "SELECT g.id_gasto,p.nombre_producto,g.cantidad,"
                + "NVL(g.unidad_medida,p.unidad_medida),"
                + "g.valor_pagado,g.forma_adquisicion "
                + "FROM gasto g "
                + "JOIN producto p ON p.id_producto=g.id_producto "
                + "WHERE g.id_visita=? ORDER BY g.id_gasto",
                v.id
            );

            Estilo.mostrar(
                tabla, m,
                "ID", "Producto", "Cantidad", "Unidad",
                "Valor pagado", "Forma de adquisicion"
            );

            Number total = (Number) Conexion.valor(
                "SELECT NVL(SUM(valor_pagado),0) "
                + "FROM gasto WHERE id_visita=?",
                v.id
            );

            lblTotal.setText(
                String.format(
                    "Total de la jornada: $ %,.2f",
                    total.doubleValue()
                )
            );

        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }

    private void actualizarPrecio() {
        boolean regalo =
                "Regalo".equals(cboForma.getSelectedItem());

        txtValor.setEnabled(!regalo);

        if (regalo) {
            txtValor.setText("0");
        }

        txtValor.setToolTipText(
            regalo
                ? "Regalo: no hay pago; se guarda cero."
                : "Valor pagado, mayor que cero."
        );
    }

    private void validarHogar(Connection cn)
            throws SQLException {

        if (!"Encuestador".equals(Sesion.rol)) {
            throw new SQLException(
                "Solo el encuestador puede modificar gastos."
            );
        }

        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT estado,id_encuestador_asignado "
            + "FROM hogar WHERE id_hogar=? FOR UPDATE NOWAIT"
        )) {
            ps.setInt(1, idHogar);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()
                        || !"Asignado".equals(rs.getString(1))
                        || rs.getInt(2) != Sesion.id) {
                    throw new SQLException(
                        "El hogar esta cerrado o no esta "
                        + "asignado a tu usuario."
                    );
                }
            }
        }

        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT COUNT(*) FROM caso "
            + "WHERE id_hogar=? AND estado_caso='Aprobado'"
        )) {
            ps.setInt(1, idHogar);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();

                if (rs.getInt(1) != 0) {
                    throw new SQLException(
                        "El hogar tiene un caso aprobado."
                    );
                }
            }
        }
    }

    private String unidadSeleccionada() {
        String unidad = UnidadesMedida.canonica((String) cboUnidad.getSelectedItem());
        if (unidad == null) throw new IllegalArgumentException("Selecciona una unidad de medida de la lista.");
        return unidad;
    }

    static BigDecimal decimal(
            String texto, boolean cero, String maximo) {

        try {
            BigDecimal n = new BigDecimal(
                texto.trim().replace(',', '.')
            ).setScale(2, RoundingMode.UNNECESSARY);

            if (n.signum() < 0
                    || (!cero && n.signum() == 0)
                    || n.compareTo(new BigDecimal(maximo)) > 0) {
                throw new NumberFormatException();
            }

            return n;

        } catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalArgumentException(
                "Numero invalido: usa "
                + (cero ? "cero o un valor positivo" : "un valor positivo")
                + ", hasta dos decimales y sin separadores de miles. "
                + "Maximo: " + maximo
            );
        }
    }

    private void guardar() {
        Item p = (Item) cboProducto.getSelectedItem();

        try {
            if (p == null) {
                throw new IllegalArgumentException(
                    "Selecciona un producto o crealo con Otro..."
                );
            }

            String unidad = unidadSeleccionada();

            BigDecimal cantidad = decimal(
                txtCantidad.getText(), false, "99999999.99"
            );

            if (cboForma.getSelectedItem() == null) {
                throw new IllegalArgumentException("Selecciona la forma de adquisición.");
            }
            String forma = String.valueOf(cboForma.getSelectedItem());

            BigDecimal valor =
                "Regalo".equals(forma)
                    ? BigDecimal.ZERO
                    : decimal(
                        txtValor.getText(),
                        false,
                        "9999999999.99"
                    );

            // Nuevas adquisiciones pertenecen al hogar; al corregir se conserva el historial.
            Integer miembro = original == null ? null : original.miembro;

            LocalDate fecha = selectorFecha.getFecha();

            int visita =
                original == null
                    ? VisitaServicio.obtenerOCrear(idHogar, fecha)
                    : original.visita;

            int dia =
                VisitaServicio.diaDelCiclo(inicioCiclo, fecha);

            try (Connection cn = Conexion.obtener()) {
                cn.setAutoCommit(false);

                try {
                    validarHogar(cn);

                    try (PreparedStatement ps = cn.prepareStatement(
                        "SELECT id_visita FROM visita "
                        + "WHERE id_visita=? AND id_hogar=?"
                    )) {
                        ps.setInt(1, visita);
                        ps.setInt(2, idHogar);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException(
                                    "La visita ya no pertenece al hogar."
                                );
                            }
                        }
                    }

                    GastoDiario.exigirDisponible(cn, idHogar, fecha, p.id,
                        original == null ? 0 : original.gasto,
                        original != null && original.producto == p.id);

                    String sql;

                    if (original == null) {
                        sql =
                            "INSERT INTO gasto "
                            + "(id_gasto,id_producto,id_miembro,cantidad,"
                            + "valor_pagado,forma_adquisicion,"
                            + "unidad_medida,id_visita) "
                            + "VALUES(seq_gasto.NEXTVAL,?,?,?,?,?,?,?)";
                    } else {
                        sql =
                            "UPDATE gasto SET id_producto=?,"
                            + "id_miembro=?,cantidad=?,valor_pagado=?,"
                            + "forma_adquisicion=?,unidad_medida=? "
                            + "WHERE id_visita=? AND id_gasto=? "
                            + "AND id_producto=? "
                            + "AND NVL(id_miembro,-1)=? "
                            + "AND cantidad=? AND valor_pagado=? "
                            + "AND forma_adquisicion=? "
                            + "AND (unidad_medida=? OR "
                            + "(unidad_medida IS NULL AND ? IS NULL))";
                    }

                    try (PreparedStatement ps =
                            cn.prepareStatement(sql)) {

                        ps.setInt(1, p.id);

                        if (miembro == null) {
                            ps.setNull(2, Types.NUMERIC);
                        } else {
                            ps.setInt(2, miembro);
                        }

                        ps.setBigDecimal(3, cantidad);
                        ps.setBigDecimal(4, valor);
                        ps.setString(5, forma);
                        ps.setString(6, unidad);
                        ps.setInt(7, visita);

                        if (original != null) {
                            ps.setInt(8, original.gasto);
                            ps.setInt(9, original.producto);
                            ps.setInt(
                                10,
                                original.miembro == null
                                    ? -1 : original.miembro
                            );
                            ps.setBigDecimal(11, original.cantidad);
                            ps.setBigDecimal(12, original.valor);
                            ps.setString(13, original.forma);
                            ps.setString(14, original.unidad);
                            ps.setString(15, original.unidad);
                        }

                        if (ps.executeUpdate() != 1) {
                            throw new SQLException(
                                "El gasto fue modificado o eliminado "
                                + "desde otra ventana. Cancela la "
                                + "edicion y vuelve a seleccionarlo."
                            );
                        }
                    }

                    if (original == null) Auditoria.alta(cn, "GASTO");
                    else Auditoria.registrar(cn, "GASTO", original.gasto, "EDITAR");
                    cn.commit();

                } catch (SQLException | RuntimeException ex) {
                    revertir(cn, ex);
                    throw ex;
                }
            }

            terminarEdicion();
            cargarVisitas(dia);
            Conexion.info(this, "Adquisición registrada correctamente.");

        } catch (GastoDiario.YaRegistrado ex) {
            ofrecerCorreccion(ex, selectorFecha.getFecha());
        } catch (SQLException | IllegalArgumentException ex) {
            Conexion.mostrarError(this, ex);
        }
    }

    private void ofrecerCorreccion(GastoDiario.YaRegistrado duplicado, LocalDate fecha) {
        int opcion = JOptionPane.showConfirmDialog(this,
            "Ese producto ya está registrado para este hogar y fecha.\n"
            + "No se guardó otro registro. ¿Deseas corregir el existente?",
            "Producto ya registrado", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) return;
        try {
            terminarEdicion();
            cargarVisitas(VisitaServicio.diaDelCiclo(inicioCiclo, fecha));
            for (int i = 0; i < cboVisita.getItemCount(); i++) {
                if (cboVisita.getItemAt(i).id == duplicado.visita) {
                    cboVisita.setSelectedIndex(i); break;
                }
            }
            cargarGastos();
            for (int i = 0; i < tabla.getRowCount(); i++) {
                if (((Number)tabla.getValueAt(i,0)).intValue() == duplicado.gasto) {
                    tabla.setRowSelectionInterval(i,i); editar(); return;
                }
            }
            Conexion.info(this,"El registro cambió desde otra ventana. Vuelve a abrir la jornada.");
        } catch (SQLException ex) { Conexion.mostrarError(this,ex); }
    }

    private void terminarEdicion() {
        original = null;

        cboVisita.setEnabled(true);
        btnNuevaVisita.setEnabled(true);
        habilitarFecha(selectorFecha, true);

        btnCancelar.setEnabled(false);
        btnGuardar.setText("Registrar adquisición");

        txtCantidad.setText("");
        txtValor.setText("");
        cboForma.setSelectedIndex(-1);
        actualizarPrecio();
        cargarGastos();
    }

    private void habilitarFecha(
            java.awt.Component c, boolean habilitar) {

        c.setEnabled(habilitar);

        if (c instanceof java.awt.Container) {
            for (java.awt.Component hijo :
                    ((java.awt.Container) c).getComponents()) {
                habilitarFecha(hijo, habilitar);
            }
        }
    }

    private void editar() {
        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            Conexion.info(
                this, "Selecciona un gasto de la tabla."
            );
            return;
        }

        int id =
            ((Number) tabla.getValueAt(fila, 0)).intValue();

        terminarEdicion();

        try (Connection cn = Conexion.obtener()) {
            try (PreparedStatement ps = cn.prepareStatement(
                "SELECT g.id_visita,g.id_producto,g.id_miembro,"
                + "g.cantidad,g.valor_pagado,g.forma_adquisicion,"
                + "g.unidad_medida,p.id_categoria,p.unidad_medida "
                + "FROM gasto g "
                + "JOIN visita v ON v.id_visita=g.id_visita "
                + "JOIN producto p ON p.id_producto=g.id_producto "
                + "WHERE g.id_gasto=? AND v.id_hogar=?"
            )) {
                ps.setInt(1, id);
                ps.setInt(2, idHogar);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException(
                            "El gasto ya no existe en este hogar."
                        );
                    }

                    Datos d = new Datos();
                    d.gasto = id;
                    d.visita = rs.getInt(1);
                    d.producto = rs.getInt(2);

                    int miembro = rs.getInt(3);
                    d.miembro = rs.wasNull() ? null : miembro;

                    d.cantidad = rs.getBigDecimal(4);
                    d.valor = rs.getBigDecimal(5);
                    d.forma = rs.getString(6);
                    d.unidad = rs.getString(7);

                    cargando = true;

                    try {
                        if (!seleccionarItem(
                                cboCategoria, rs.getInt(8))) {
                            throw new SQLException(
                                "Actualiza las categorias antes de editar."
                            );
                        }

                        cargarProductos(d.producto);
                        cargando = true;

                        if (!seleccionarItem(
                                cboProducto, d.producto)) {
                            throw new SQLException(
                                "Actualiza los productos antes de editar."
                            );
                        }

                        UnidadesMedida.seleccionar(cboUnidad,
                            d.unidad == null
                                ? rs.getString(9)
                                : d.unidad
                        );

                        cboForma.setSelectedItem(d.forma);
                        txtCantidad.setText(
                            d.cantidad.toPlainString()
                        );
                        txtValor.setText(
                            d.valor.toPlainString()
                        );

                        actualizarPrecio();

                    } finally {
                        cargando = false;
                    }

                    original = d;
                }
            }

            cboVisita.setEnabled(false);
            btnNuevaVisita.setEnabled(false);
            habilitarFecha(selectorFecha, false);

            btnCancelar.setEnabled(true);
            btnGuardar.setText(
                "Confirmar corrección " + id
            );

        } catch (SQLException ex) {
            terminarEdicion();
            Conexion.mostrarError(this, ex);
        }
    }

    private void otroProducto() {
        Item cat = (Item) cboCategoria.getSelectedItem();

        if (cat == null) {
            Conexion.info(
                this, "Selecciona primero una categoria."
            );
            return;
        }

        JTextField nombre = new JTextField(24);
        JComboBox<String> unidad = UnidadesMedida.combo();
        UnidadesMedida.seleccionar(unidad, (String) cboUnidad.getSelectedItem());

        JPanel panel = new JPanel();
        Estilo.fila(
            panel, 0, "Categoria", new JLabel(cat.toString())
        );
        Estilo.fila(panel, 1, "Nombre del producto", nombre);
        Estilo.fila(panel, 2, "Unidad habitual", unidad);

        int respuesta = JOptionPane.showConfirmDialog(
            this, panel, "Otro producto",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }

        String n = nombre.getText().trim();
        String u = UnidadesMedida.canonica((String) unidad.getSelectedItem());

        if (n.isEmpty() || n.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 100 || u == null) {
            Conexion.info(this, "Escribe un nombre de producto válido y elige su unidad habitual de la lista.");
            return;
        }

        try {
            int id;

            try (Connection cn = Conexion.obtener()) {
                cn.setAutoCommit(false);

                try {
                    validarHogar(cn);

                    try (Statement st = cn.createStatement()) {
                        st.executeUpdate(
                            "LOCK TABLE producto "
                            + "IN SHARE ROW EXCLUSIVE MODE NOWAIT"
                        );
                    }

                    id = buscarProducto(cn, cat.id, n, u);

                    if (id == 0) {
                        long siguiente;

                        try (
                            Statement st = cn.createStatement();
                            ResultSet rs = st.executeQuery(
                                "SELECT seq_producto.NEXTVAL "
                                + "FROM dual"
                            )
                        ) {
                            rs.next();
                            siguiente = rs.getLong(1);
                        }

                        if (siguiente > Integer.MAX_VALUE) {
                            throw new SQLException(
                                "El identificador supera el rango "
                                + "de esta aplicacion."
                            );
                        }

                        try (PreparedStatement ps = cn.prepareStatement(
                            "INSERT INTO producto "
                            + "(id_producto,id_categoria,"
                            + "nombre_producto,unidad_medida) "
                            + "VALUES(?,?,?,?)"
                        )) {
                            ps.setLong(1, siguiente);
                            ps.setInt(2, cat.id);
                            ps.setString(3, n);
                            ps.setString(4, u);
                            ps.executeUpdate();
                        }

                        id = buscarProducto(cn, cat.id, n, u);
                        if (id != 0) Auditoria.registrar(cn, "PRODUCTO", id, "CREAR");

                        if (id == 0) {
                            throw new SQLException(
                                "No se pudo recuperar el producto creado."
                            );
                        }
                    }

                    cn.commit();

                } catch (SQLException | RuntimeException ex) {
                    revertir(cn, ex);
                    throw ex;
                }
            }

            cargarProductos(id);

            UnidadesMedida.seleccionar(cboUnidad, u);

            Conexion.info(
                this,
                "Producto disponible en la lista.\n"
                + "El gasto se guarda con el boton Guardar."
            );

        } catch (SQLException ex) {
            Conexion.mostrarError(this, ex);
        }
    }

    private int buscarProducto(
            Connection cn, int categoria,
            String nombre, String unidad) throws SQLException {

        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT id_producto FROM producto "
            + "WHERE id_categoria=? AND nombre_producto=? "
            + "AND unidad_medida=? ORDER BY id_producto"
        )) {
            ps.setInt(1, categoria);
            ps.setString(2, nombre);
            ps.setString(3, unidad);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void revertir(
            Connection cn, Exception ex) {

        try {
            cn.rollback();
        } catch (SQLException rollback) {
            ex.addSuppressed(rollback);
        }
    }
}

