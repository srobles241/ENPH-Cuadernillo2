package enph;
import java.sql.*;
import java.util.*;

final class CuentasAdmin {
    enum Rol {
        Encuestador("encuestador"), Recuentista("recuentista"), Supervisor("supervisor");
        final String tabla,col;
        Rol(String t){tabla=t;col="id_"+t;}
    }
    static final class Perfil {
        final Rol rol;final int id;final String nombre;
        Perfil(Rol r,int i,String n){rol=r;id=i;nombre=n;}
        public String toString(){return nombre+" · "+rol+" #"+id;}
    }
    static void exigirAdmin(){
        if(!Sesion.esAdmin() || !"Administrador".equals(Sesion.rol))
            throw new SecurityException("Acción reservada a administración.");
    }
    static String login(String raw){
        String s=raw.trim().toLowerCase(Locale.ROOT);
        if(!s.matches("[a-z0-9_.-]{3,50}")||s.equals("administrador"))
            throw new IllegalArgumentException("Usuario de 3 a 50 caracteres: letras, números, punto, guion o guion bajo.");
        return s;
    }
    static String nombre(String n){
        n=n.trim();
        if(n.isEmpty()||n.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>100)
            throw new IllegalArgumentException("Escribe un nombre completo más corto.");
        return n;
    }
    static List<Perfil> perfiles(Rol rol,boolean libres)throws SQLException {
        exigirAdmin();
        List<Perfil> out=new ArrayList<>();
        String sql="SELECT p."+rol.col+",p.nombre_completo FROM "+rol.tabla+" p "
            +(libres?"WHERE NOT EXISTS (SELECT 1 FROM enph_usuario u WHERE u."+rol.col+"=p."+rol.col+") ":"")
            +"ORDER BY p.nombre_completo";
        try(Connection c=Conexion.obtener();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){
            while(r.next())out.add(new Perfil(rol,r.getInt(1),r.getString(2)));
        }
        return out;
    }
    static void crear(Rol rol,Perfil perfil,String usuario,String nombre,char[] clave)throws SQLException {
        exigirAdmin();usuario=login(usuario);nombre=nombre(nombre);String hash=ClaveAcceso.nueva(clave); String esquema=EsquemasOracle.nombre(usuario);
        try(Connection c=Conexion.obtener()){
            c.setAutoCommit(false);
            try {
                int id;
                if(perfil==null){
                    String sql="INSERT INTO "+rol.tabla+" ("+rol.col+",nombre_completo,"+rol.tabla+"_login,contrasena) VALUES (seq_"+rol.tabla+".NEXTVAL,?,?,?)";
                    String interno="perfil_"+UUID.randomUUID().toString().replace("-","");
                    try(PreparedStatement p=c.prepareStatement(sql)){
                        p.setString(1,nombre);p.setString(2,interno);p.setString(3,"SIN_ACCESO_LEGADO");p.executeUpdate();
                    }
                    Auditoria.alta(c,rol.tabla.toUpperCase(java.util.Locale.ROOT));
                    try(PreparedStatement p=c.prepareStatement("SELECT "+rol.col+" FROM "+rol.tabla+" WHERE "+rol.tabla+"_login=?")){
                        p.setString(1,interno);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("No se creó el perfil.");id=r.getInt(1);}
                    }
                }else{
                    if(perfil.rol!=rol)throw new SQLException("El perfil no corresponde al rol.");
                    id=perfil.id;
                }
                try(PreparedStatement p=c.prepareStatement(
                    "INSERT INTO enph_usuario(login,nombre,rol,clave_hash,activo,"+rol.col+",usuario_oracle) VALUES(?,?,?,?,'S',?,?)")){
                    p.setString(1,usuario);p.setString(2,nombre);p.setString(3,rol.name());p.setString(4,hash);p.setInt(5,id);p.setString(6,esquema);p.executeUpdate();
                }
                nombrePerfil(c,rol,id,nombre);
                Auditoria.registrar(c,"ENPH_USUARIO",usuario,"CREAR");
                EsquemasOracle.crear(esquema,rol);
                try{c.commit();}catch(SQLException e){throw new SQLException("Revisa el estado de la cuenta y del esquema bloqueado "+esquema+" antes de reintentar.",e);}
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    private static void nombrePerfil(Connection c,Rol r,int id,String nombre)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("UPDATE "+r.tabla+" SET nombre_completo=? WHERE "+r.col+"=?")){
            p.setString(1,nombre);p.setInt(2,id);
            if(p.executeUpdate()!=1)throw new SQLException("Perfil inexistente.");
            Auditoria.registrar(c,r.tabla.toUpperCase(java.util.Locale.ROOT),id,"EDITAR");
        }
    }
    static void editar(String original,String usuario,String nombre,char[] clave,String activo)throws SQLException {
        exigirAdmin();usuario=login(usuario);nombre=nombre(nombre);
        if(!"S".equals(activo)&&!"N".equals(activo))throw new IllegalArgumentException("Estado inválido.");
        String hash=clave.length==0?null:ClaveAcceso.nueva(clave);
        try(Connection c=Conexion.obtener()){
            c.setAutoCommit(false);
            try{
                Rol rol;int id;String estadoAnterior;
                try(PreparedStatement p=c.prepareStatement("SELECT rol,COALESCE(id_encuestador,id_recuentista,id_supervisor),activo FROM enph_usuario WHERE login=? AND rol<>'Administrador' FOR UPDATE")){
                    p.setString(1,original);
                    try(ResultSet r=p.executeQuery()){
                        if(!r.next())throw new SQLException("Cuenta operativa inexistente.");
                        rol=Rol.valueOf(r.getString(1));id=r.getInt(2);estadoAnterior=r.getString(3);
                    }
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE enph_usuario SET login=?,nombre=?,activo=?,clave_hash=NVL(?,clave_hash) WHERE login=?")){
                    p.setString(1,usuario);p.setString(2,nombre);p.setString(3,activo);p.setString(4,hash);p.setString(5,original);p.executeUpdate();
                }
                String accion=activo.equals(estadoAnterior)?"EDITAR":"S".equals(activo)?"REACTIVAR":"DESACTIVAR";
                Auditoria.registrar(c,"ENPH_USUARIO",original.equals(usuario)?usuario:original+" -> "+usuario,accion);
                nombrePerfil(c,rol,id,nombre);c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    static final String SQL_ESTADO="UPDATE enph_usuario SET activo=? WHERE login=? AND rol<>'Administrador' AND activo=?";
    static void cambiarEstado(String usuario,boolean activar)throws SQLException {
        exigirAdmin();
        if(Conexion.ejecutarAuditado("ENPH_USUARIO",login(usuario),activar?"REACTIVAR":"DESACTIVAR",SQL_ESTADO,activar?"S":"N",login(usuario),activar?"N":"S")!=1)
            throw new SQLException("La cuenta no existe o su estado ya cambió. Recarga la lista.");
    }
    static void cambiarClaveAdmin(char[] anterior,char[] nueva)throws SQLException {
        exigirAdmin();
        String hash=ClaveAcceso.nueva(nueva);
        try(Connection c=Conexion.obtener()){
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement("SELECT clave_hash FROM enph_usuario WHERE login='administrador' AND rol='Administrador' FOR UPDATE")){
                try(ResultSet r=p.executeQuery()){
                    if(!r.next()||!ClaveAcceso.coincide(anterior,r.getString(1)))throw new SQLException("La contraseña actual no coincide.");
                }
                try(PreparedStatement u=c.prepareStatement("UPDATE enph_usuario SET clave_hash=? WHERE login='administrador'")){
                    u.setString(1,hash);u.executeUpdate();
                    Auditoria.registrar(c,"ENPH_USUARIO","administrador","EDITAR");
                }
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
}

