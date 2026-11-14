package enph;
import java.sql.*;
import java.util.Locale;

public final class Autenticacion {
    public static final class Usuario {
        public final int id;
        public final String nombre,rol,login;
        Usuario(int id,String nombre,String rol,String login){
            this.id=id;this.nombre=nombre;this.rol=rol;this.login=login;
        }
    }
    public static Usuario ingresar(String login,char[] clave)throws SQLException {
        String sql="SELECT login,nombre,rol,clave_hash,COALESCE(id_encuestador,id_recuentista,id_supervisor,0) "
            +"FROM enph_usuario WHERE login=? AND activo='S'";
        try(Connection c=Conexion.obtener();PreparedStatement p=c.prepareStatement(sql)){
            p.setString(1,login.trim().toLowerCase(Locale.ROOT));
            try(ResultSet r=p.executeQuery()){
                if(!r.next()||!ClaveAcceso.coincide(clave,r.getString(4)))return null;
                return new Usuario(r.getInt(5),r.getString(2),r.getString(3),r.getString(1));
            }
        }catch(SQLException e){
            if(e.getErrorCode()==942)throw new SQLException("Falta instalar la migración de cuentas ENPH.",e);
            throw e;
        }
    }
}
