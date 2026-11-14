package enph;

import java.awt.Component;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/** Conexión JDBC a Oracle 10g y métodos de acceso a datos reutilizables. */
public class Conexion {

    private static final java.util.Properties CONFIG = new java.util.Properties();
    static {
        try (java.io.InputStream in = new java.io.FileInputStream("conexion.local.properties")) {
            CONFIG.load(in);
        } catch (java.io.IOException ignored) { }
    }
    // DOCUMENTACION: Abre una conexion JDBC a Oracle. Las propiedades enph.url, enph.usuario y enph.clave tienen prioridad sobre el archivo local; si falta el usuario, lanza SQLException.
    public static Connection obtener() throws SQLException {
        try { Class.forName("oracle.jdbc.OracleDriver"); }
        catch (ClassNotFoundException e) { throw new SQLException("Falta lib/ojdbc14.jar en el proyecto."); }
        String url = System.getProperty("enph.url", CONFIG.getProperty("url", "jdbc:oracle:thin:@localhost:1521:orcl"));
        String usuario = System.getProperty("enph.usuario", CONFIG.getProperty("usuario", ""));
        String clave = System.getProperty("enph.clave", CONFIG.getProperty("clave", ""));
        if (usuario.isEmpty()) throw new SQLException("Configura conexion.local.properties antes de ingresar.");
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", usuario);
        props.setProperty("password", clave);
        props.setProperty("oracle.net.CONNECT_TIMEOUT", "8000");
        props.setProperty("oracle.jdbc.ReadTimeout", "15000");
       DriverManager.setLoginTimeout(8);


    Connection cn=DriverManager.getConnection(url, props);
        try {
            String marca=Sesion.marcaAuditoria();
            if (marca!=null) identificar(cn,marca);
            return cn;
        } catch (SQLException e) {
            try{cn.close();}catch(SQLException close){e.addSuppressed(close);}
            throw e;
        }
    }

    // DOCUMENTACION: Asocia los parametros al PreparedStatement en orden, desde la posicion 1; envia los valores nulos como VARCHAR.
    static void identificar(Connection cn,String marca)throws SQLException {
        try(java.sql.CallableStatement p=cn.prepareCall("BEGIN DBMS_SESSION.SET_IDENTIFIER(?); END;")){
            p.setString(1,marca);p.execute();
        }
    }

    private static void asignar(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                ps.setNull(i + 1, Types.VARCHAR);
            } else {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    /** Ejecuta un SELECT y devuelve un modelo de tabla (no editable). */
    // DOCUMENTACION: Ejecuta una consulta parametrizada, convierte fechas a dd/MM/yyyy y devuelve un modelo de tabla no editable.
    public static DefaultTableModel consultar(String sql, Object... params) throws SQLException {
        try (Connection cn = obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                Vector<String> cols = new Vector<String>();
                for (int i = 1; i <= n; i++) {
                    cols.add(md.getColumnLabel(i));
                }
                SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
                Vector<Vector<Object>> filas = new Vector<Vector<Object>>();
                while (rs.next()) {
                    Vector<Object> fila = new Vector<Object>();
                    for (int i = 1; i <= n; i++) {
                        Object v = rs.getObject(i);
                        if (v instanceof java.util.Date) {
                            v = f.format((java.util.Date) v);
                        }
                        fila.add(v);
                    }
                    filas.add(fila);
                }
                return new DefaultTableModel(filas, cols) {
                    @Override
                    // DOCUMENTACION: Impide editar las celdas del resultado de una consulta.
                    public boolean isCellEditable(int r, int c) {
                        return false;
                    }
                };
            }
        }
    }

    /** Devuelve la primera columna de la primera fila (o null). */
    // DOCUMENTACION: Devuelve la primera columna de la primera fila, o null si no existen resultados.
    public static Object valor(String sql, Object... params) throws SQLException {
        try (Connection cn = obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        }
    }

    /** INSERT / UPDATE / DELETE con commit. Devuelve filas afectadas. */
    // DOCUMENTACION: Ejecuta una sentencia de modificacion, confirma la transaccion y devuelve las filas afectadas.
    public static int ejecutar(String sql, Object... params) throws SQLException {
        try (Connection cn = obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, params);
            cn.setAutoCommit(false);
            int n = ps.executeUpdate();
            cn.commit();
            return n;
        }
    }

    static int ejecutarAuditado(String tabla, Object id, String accion, String sql, Object... params) throws SQLException {
        try (Connection cn = obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            cn.setAutoCommit(false);
            try {
                asignar(ps, params);
                int filas = ps.executeUpdate();
                if (filas > 0) {
                    if (id == null) Auditoria.alta(cn, tabla);
                    else Auditoria.registrar(cn, tabla, id, accion);
                }
                cn.commit();
                return filas;
            } catch (SQLException | RuntimeException e) {
                try { cn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    /** Ejecuta varias sentencias como una sola transacción (todo o nada). */
    // DOCUMENTACION: Ejecuta las sentencias en una sola conexion; confirma todas o revierte si se produce SQLException.
    public static void transaccion(String[] sqls, Object[][] params) throws SQLException {
        try (Connection cn = obtener()) {
            cn.setAutoCommit(false);
            try {
                for (int i = 0; i < sqls.length; i++) {
                    try (PreparedStatement ps = cn.prepareStatement(sqls[i])) {
                        asignar(ps, params[i]);
                        ps.executeUpdate();
                    }
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    /**
     * Llena un combo con un SELECT: col 1 = id, col 2 = texto, col 3 (opcional) = extra.
     */
    // DOCUMENTACION: Carga elementos Item desde las columnas identificador, texto y dato extra opcional de la consulta.
    public static void cargarCombo(JComboBox<Item> combo, String sql, Object... params) throws SQLException {
        try (Connection cn = obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                combo.removeAllItems();
                while (rs.next()) {
                    combo.addItem(new Item(rs.getInt(1), rs.getString(2), n >= 3 ? rs.getString(3) : ""));
                }
            }
        }
    }

    // DOCUMENTACION: Muestra el mensaje del error y explica algunas restricciones de Oracle: CHECK, unicidad y longitud.
    public static void mostrarError(Component padre, Exception ex) {
        String m = ex.getMessage();
        if (ex instanceof SQLException) {
            int c = ((SQLException) ex).getErrorCode();
            if (c == 2290) {
                m = "La base de datos rechazó el dato por no cumplir una regla de validación "
                        + "(por ejemplo, cantidad o valor menores o iguales a cero).\n\n" + m;
            } else if (c == 1) {
                m = "Ya existe un registro con ese valor único.\n\n" + m;
            } else if (c == 12899) {
                m = "Un texto excede la longitud permitida en la base de datos.\n\n" + m;
            }
        }
        JOptionPane.showMessageDialog(padre, m, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // DOCUMENTACION: Muestra un mensaje informativo con el titulo ENPH.
    public static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "ENPH", JOptionPane.INFORMATION_MESSAGE);
    }
}
