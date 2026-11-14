package enph;
import java.awt.Component;
import java.util.Arrays;
import javax.swing.*;

/** Conserva el formulario mientras se corrige la contraseña. */
final class DialogoClave {
    static char[] pedir(Component padre,JPanel panel,String titulo,
                        JPasswordField clave,JPasswordField confirmacion,boolean opcional){
        while(JOptionPane.showConfirmDialog(padre,panel,titulo,JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
            char[] a=clave.getPassword(),b=confirmacion.getPassword();
            clave.setText("");confirmacion.setText("");
            try{
                if(!opcional||a.length!=0)ClaveAcceso.validar(a);
                if(!Arrays.equals(a,b))throw new IllegalArgumentException("Las contraseñas no coinciden.");
                return a;
            }catch(IllegalArgumentException e){
                Arrays.fill(a,'\0');
                JOptionPane.showMessageDialog(padre,e.getMessage(),titulo,JOptionPane.WARNING_MESSAGE);
            }finally{Arrays.fill(b,'\0');}
        }
        clave.setText("");confirmacion.setText("");
        return null;
    }
}
