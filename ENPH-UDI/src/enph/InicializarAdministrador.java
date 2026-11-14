package enph;
import java.sql.*;
import java.util.Arrays;
import javax.swing.*;

/** Herramienta de instalación: ejecutar manualmente desde la estación propietaria. */
public final class InicializarAdministrador {
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            JPasswordField clave=new JPasswordField(24),confirmar=new JPasswordField(24);
            JPanel p=new JPanel();
            Estilo.fila(p,0,"Cuenta inicial",new JLabel("administrador"));
            Estilo.fila(p,1,"Contraseña (mínimo "+ClaveAcceso.MINIMO+")",clave);
            Estilo.fila(p,2,"Confirmar contraseña",confirmar);
            char[] a=DialogoClave.pedir(null,p,"Instalar cuenta de administración",clave,confirmar,false);
            if(a==null)return;
            try{
                inicializar(a);
                Conexion.info(null,"Administrador creado. Abre LoginFrame e ingresa como administrador.");
            }catch(Exception e){Conexion.mostrarError(null,e);}
            finally{Arrays.fill(a,'\0');}
        });
    }
    static void inicializar(char[] clave)throws SQLException{
        String hash=ClaveAcceso.nueva(clave);
        try(Connection c=Conexion.obtener()){
            c.setAutoCommit(false);
            try{
                Conexion.identificar(c,"ENPHADM|administrador|Inicializacion|0");
                try(PreparedStatement p=c.prepareStatement("INSERT INTO enph_usuario(login,nombre,rol,clave_hash,activo) VALUES ('administrador','Administrador','Administrador',?,'S')")){
                    p.setString(1,hash);p.executeUpdate();
                    Auditoria.registrar(c,"ENPH_USUARIO","administrador","CREAR","ENPHADM|administrador|Inicializacion|0");
                }
                c.commit();
            }catch(SQLException e){
                c.rollback();
                if(e.getErrorCode()==1)throw new SQLException("Ya existe un administrador; esta herramienta no restablece su contraseña.",e);
                throw e;
            }
        }
    }
}
