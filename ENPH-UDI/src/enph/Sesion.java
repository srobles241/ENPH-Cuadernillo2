package enph;

import java.security.MessageDigest;

/** Datos del usuario autenticado y utilidad para cifrar contraseñas (SHA-256). */
public class Sesion {
    public static String rol;
    public static int id;
    public static String nombre;

    private static String administrador;
    public static boolean esAdmin() { return administrador != null; }
    public static void iniciar(Autenticacion.Usuario u) {
        cerrar(); id=u.id; nombre=u.nombre; rol=u.rol;
        if ("Administrador".equals(rol)) administrador=u.login;
    }
    public static void actuar(String nuevoRol,int nuevoId,String nuevoNombre) {
        CuentasAdmin.exigirAdmin(); rol=nuevoRol; id=nuevoId; nombre=nuevoNombre;
    }
    public static void volverAdmin(String nombreAdmin) {
        if (!esAdmin()) throw new SecurityException("Sesión de administrador terminada.");
        rol="Administrador"; id=0; nombre=nombreAdmin;
    }
    static String marcaAuditoria() {
        return esAdmin() ? "ENPHADM|"+administrador+"|"+rol+"|"+id : null;
    }


    // DOCUMENTACION: Limpia el identificador, nombre y rol del usuario activo.
    public static void cerrar() {
        administrador = null;
        rol = null;
        id = 0;
        nombre = null;
    }

    /** Devuelve el hash SHA-256 en hexadecimal (64 caracteres, cabe en VARCHAR2(255)). */
    // DOCUMENTACION: Calcula SHA-256 del texto codificado en UTF-8 y devuelve 64 caracteres hexadecimales; no permite recuperar la clave original.
    public static String hash(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(texto.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
