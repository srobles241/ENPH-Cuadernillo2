package enph;
import java.sql.*;
import java.util.UUID;

final class EsquemasOracle {
    static String nombre(String login){
        String limpio=login.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9_]","_");
        if(limpio.length()>14)limpio=limpio.substring(0,14);
        return "ENPH_U_"+limpio+"_"+Sesion.hash(login).substring(0,8).toUpperCase(java.util.Locale.ROOT);
    }
    static void crear(String esquema,CuentasAdmin.Rol rol)throws SQLException{
        CuentasAdmin.exigirAdmin();
        // El DDL de Oracle confirma implícitamente: usar otra conexión.
        String clave="Z"+UUID.randomUUID().toString().replace("-","").substring(0,25);
        try(Connection c=Conexion.obtener();CallableStatement p=c.prepareCall("BEGIN enph_provisionar_esquema(?,?,?); END;")){
            p.setString(1,esquema);p.setString(2,rol.name());p.setString(3,clave);p.execute();
        }
    }
}
