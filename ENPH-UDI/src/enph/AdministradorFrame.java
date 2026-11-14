package enph;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

final class AdministradorFrame extends JFrame {
    private final JTable tabla=Estilo.tabla();
    private JButton botonEstado;
    AdministradorFrame(){
        super("ENPH · Administración");
        CuentasAdmin.exigirAdmin();
        setDefaultCloseOperation(EXIT_ON_CLOSE);setSize(1000,540);setLocationRelativeTo(null);
        add(Estilo.encabezado("Administración del operativo"," "),BorderLayout.NORTH);
        add(new JScrollPane(tabla),BorderLayout.CENTER);
        JPanel acciones=new JPanel(new GridLayout(0,3,8,8));
        boton(acciones,"Crear cuenta",()->formulario(false));
        boton(acciones,"Editar cuenta / restablecer clave",()->formulario(true));
        botonEstado=boton(acciones,"Desactivar cuenta",this::cambiarEstado);
        botonEstado.setEnabled(false);
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotonEstado());
        boton(acciones,"Actuar en un rol",this::actuar);
        boton(acciones,"Ver historial",this::historial);
        boton(acciones,"Cambiar mi contraseña",this::cambiarClave);
        boton(acciones,"Recargar cuentas",this::cargar);
        boton(acciones,"Cerrar sesión",()->{Sesion.cerrar();new LoginFrame().setVisible(true);dispose();});
        add(acciones,BorderLayout.SOUTH);
        try{cargar();}catch(Exception e){Conexion.mostrarError(this,e);}
    }
    private interface Accion{void ejecutar()throws Exception;}
    private JButton boton(JPanel p,String texto,Accion a){
        JButton b=new JButton(texto);p.add(b);
        b.addActionListener(e->{try{a.ejecutar();}catch(Exception ex){Conexion.mostrarError(this,ex);}});
        return b;
    }
    private void cargar()throws Exception {
        CuentasAdmin.exigirAdmin();
        Estilo.mostrar(tabla,Conexion.consultar(
            "SELECT login,nombre,rol,CASE activo WHEN 'S' THEN 'Activa' ELSE 'Desactivada' END FROM enph_usuario ORDER BY rol,login"),
            "Usuario","Nombre","Rol","Estado");
        actualizarBotonEstado();
    }
    private int seleccion(){
        int f=tabla.getSelectedRow();
        if(f<0)throw new IllegalArgumentException("Selecciona una cuenta de la lista.");
        f=tabla.convertRowIndexToModel(f);
        if("Administrador".equals(dato(f,2)))throw new IllegalArgumentException("Usa «Cambiar mi contraseña» para tu cuenta.");
        return f;
    }
    private String dato(int f,int c){return tabla.getModel().getValueAt(f,c).toString();}
    private void formulario(boolean editar)throws Exception{
        int f=editar?seleccion():-1;
        String original=editar?dato(f,0):null;
        CuentasAdmin.Rol r;
        if(editar)r=CuentasAdmin.Rol.valueOf(dato(f,2));
        else{
            r=elegirRol();if(r==null)return;
        }
        CuentasAdmin.Perfil perfil=null;
        if(!editar){
            java.util.List<CuentasAdmin.Perfil> perfiles=CuentasAdmin.perfiles(r,true);
            Object[] opciones=new Object[perfiles.size()+1];opciones[0]="Crear un perfil nuevo";
            for(int i=0;i<perfiles.size();i++)opciones[i+1]=perfiles.get(i);
            Object elegido=JOptionPane.showInputDialog(this,"Elige un perfil existente para conservar sus asignaciones, o crea uno nuevo.",
                "Perfil de "+r,JOptionPane.PLAIN_MESSAGE,null,opciones,opciones[0]);
            if(elegido==null)return;
            if(elegido instanceof CuentasAdmin.Perfil)perfil=(CuentasAdmin.Perfil)elegido;
        }
        JTextField usuario=new JTextField(editar?original:"",24);
        JTextField nombre=new JTextField(editar?dato(f,1):perfil==null?"":perfil.nombre,24);
        JPasswordField clave=new JPasswordField(24),confirmar=new JPasswordField(24);
        JCheckBox activa=new JCheckBox("Permitir ingreso",!editar||"Activa".equals(dato(f,3)));
        JPanel p=new JPanel();
        Estilo.fila(p,0,"Rol",new JLabel(r.name()));
        Estilo.fila(p,1,"Nombre completo",nombre);
        Estilo.fila(p,2,"Usuario",usuario);
        Estilo.fila(p,3,editar?"Clave nueva (opcional)":"Contraseña (mínimo "+ClaveAcceso.MINIMO+")",clave);
        Estilo.fila(p,4,"Confirmar clave",confirmar);
        if(editar)Estilo.fila(p,5,"Estado",activa);
        char[] a=DialogoClave.pedir(this,p,editar?"Editar cuenta":"Crear cuenta",clave,confirmar,editar);
        if(a==null)return;
        try{
            if(editar)CuentasAdmin.editar(original,usuario.getText(),nombre.getText(),a,activa.isSelected()?"S":"N");
            else CuentasAdmin.crear(r,perfil,usuario.getText(),nombre.getText(),a);
            cargar();
            Conexion.info(this,editar?"Cambios guardados en Oracle.":"Cuenta creada y guardada en Oracle.");
        }finally{Arrays.fill(a,'\0');}
    }
    private void actualizarBotonEstado(){
        int fila=tabla.getSelectedRow();
        boolean operativa=false,activa=false;
        if(fila>=0 && tabla.getColumnCount()>=4){
            int f=tabla.convertRowIndexToModel(fila);
            operativa=!"Administrador".equals(dato(f,2));
            activa="Activa".equals(dato(f,3));
        }
        botonEstado.setEnabled(operativa);
        botonEstado.setText(operativa&&!activa?"Reactivar cuenta":"Desactivar cuenta");
    }
    private void cambiarEstado()throws Exception {
        int f=seleccion();
        String usuario=dato(f,0);
        boolean activar=!"Activa".equals(dato(f,3));
        String accion=activar?"Reactivar":"Desactivar";
        if(JOptionPane.showConfirmDialog(this,"¿"+accion+" "+usuario+"? Sus registros del operativo se conservan.",
            "Confirmar cambio de acceso",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        CuentasAdmin.cambiarEstado(usuario,activar);
        cargar();
        Conexion.info(this,activar?"Cuenta reactivada. Puede ingresar con su contraseña.":"Cuenta desactivada. No puede iniciar sesión.");
    }
    private CuentasAdmin.Rol elegirRol(){
        return (CuentasAdmin.Rol)JOptionPane.showInputDialog(this,"Selecciona el rol operativo","Rol",
            JOptionPane.PLAIN_MESSAGE,null,CuentasAdmin.Rol.values(),CuentasAdmin.Rol.Encuestador);
    }
    private void actuar()throws Exception {
        CuentasAdmin.Rol r=elegirRol();if(r==null)return;
        Object[] perfiles=CuentasAdmin.perfiles(r,false).toArray();
        if(perfiles.length==0){Conexion.info(this,"Primero crea un perfil para este rol.");return;}
        CuentasAdmin.Perfil p=(CuentasAdmin.Perfil)JOptionPane.showInputDialog(this,
            "Elige el perfil responsable. Los cambios quedarán como «Editado por el administrador».",
            "Actuar como "+r,JOptionPane.PLAIN_MESSAGE,null,perfiles,perfiles[0]);
        if(p==null)return;
        Set<Window> anteriores=new HashSet<>(Arrays.asList(Window.getWindows()));
        String nombreAdmin=Sesion.nombre;
        Sesion.actuar(p.rol.name(),p.id,p.nombre);
        JFrame f;
        try{f=r==CuentasAdmin.Rol.Encuestador?new EncuestadorFrame():r==CuentasAdmin.Rol.Supervisor?new SupervisorFrame():new RecuentistaFrame();}
        catch(RuntimeException ex){Sesion.volverAdmin(nombreAdmin);throw ex;}
        f.setTitle(f.getTitle()+" · Actuando como administrador");
        f.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        f.addWindowListener(new WindowAdapter(){
            @Override public void windowClosed(WindowEvent e){
                for(Window w:Window.getWindows())if(!anteriores.contains(w)&&w!=f&&w.isDisplayable())w.dispose();
                Sesion.volverAdmin(nombreAdmin);
                setVisible(true);toFront();
                try{cargar();}catch(Exception ex){Conexion.mostrarError(AdministradorFrame.this,ex);}
            }
        });
        setVisible(false);f.setVisible(true);
    }
    private void historial()throws Exception {
        CuentasAdmin.exigirAdmin();
        JTable t=Estilo.tabla();
        Estilo.mostrar(t,Conexion.consultar("SELECT fecha,tabla_objeto,id_registro,accion,contexto,marca "
          +"FROM (SELECT fecha,tabla_objeto,id_registro,accion,contexto,marca FROM enph_aud_admin ORDER BY id_aud DESC) WHERE ROWNUM<=200"),
          "Fecha","Tabla","Registro","Acción","Administrador / perfil","Marca");
        JScrollPane s=new JScrollPane(t);s.setPreferredSize(new Dimension(950,360));
        JOptionPane.showMessageDialog(this,s,"Últimos 200 cambios de administración",JOptionPane.PLAIN_MESSAGE);
    }
    private void cambiarClave()throws Exception{
        JPasswordField actual=new JPasswordField(24),nueva=new JPasswordField(24),confirmar=new JPasswordField(24);
        JPanel p=new JPanel();
        Estilo.fila(p,0,"Clave actual",actual);Estilo.fila(p,1,"Clave nueva",nueva);Estilo.fila(p,2,"Confirmar clave",confirmar);
        char[] b=DialogoClave.pedir(this,p,"Cambiar mi contraseña",nueva,confirmar,false);
        if(b==null){actual.setText("");return;}
        char[] a=actual.getPassword();actual.setText("");
        try{
            CuentasAdmin.cambiarClaveAdmin(a,b);Conexion.info(this,"Contraseña actualizada.");
        }finally{Arrays.fill(a,'\0');Arrays.fill(b,'\0');}
    }
}
