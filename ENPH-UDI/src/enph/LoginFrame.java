package enph;
import java.awt.*;
import javax.swing.*;
import java.util.Arrays;

/** Inicio de sesión: determina el rol y realiza la conexión fuera del hilo gráfico. */
public class LoginFrame extends JFrame {
    private final JTextField txtUsuario = new JTextField(22);
    private final JPasswordField txtClave = new JPasswordField(22);
    private final JButton btnIngresar = new JButton("Ingresar al sistema");
    private final JLabel estado = new JLabel(" ");
    // DOCUMENTACION: Construye los componentes de esta clase, configura su estado inicial y registra los eventos definidos en el constructor.
    public LoginFrame() {
        super("ENPH | Cuadernillo 2");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(Estilo.encabezado("Encuesta de presupuestos", "ENPH  /  Cuadernillo 2  /  Hogares urbanos"), BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24,28,24,28));
        Estilo.fila(form,1,"Usuario",txtUsuario);
        Estilo.fila(form,2,"Contraseña",txtClave);
        Estilo.fila(form,3,"",btnIngresar);
        Estilo.fila(form,4," ",estado);
        add(form,BorderLayout.CENTER);
        txtUsuario.setToolTipText("Usuario del operativo ENPH");
        btnIngresar.addActionListener(e->ingresar());
        getRootPane().setDefaultButton(btnIngresar);
        setSize(610,400); setLocationRelativeTo(null); setResizable(false);
        Estilo.agregarCerrar(this);
    }
    // DOCUMENTACION: Valida campos y ejecuta la autenticacion en SwingWorker. Limpia la clave y abre la ventana correspondiente al rol.
    private void ingresar() {
        final String usuario=txtUsuario.getText().trim();
        final char[] clave=txtClave.getPassword();
        if(usuario.isEmpty() || clave.length==0) {
            Arrays.fill(clave,' ');
            JOptionPane.showMessageDialog(this,"Escribe el usuario y la contraseña."); return;
        }
        btnIngresar.setEnabled(false); estado.setText(" ");
        new SwingWorker<Autenticacion.Usuario,Void>() {
            // DOCUMENTACION: Autentica fuera del hilo grafico y borra el arreglo de caracteres de la clave al finalizar.
            protected Autenticacion.Usuario doInBackground() throws Exception {
                try {return Autenticacion.ingresar(usuario,clave);} finally {Arrays.fill(clave,' ');}
            }
            // DOCUMENTACION: Procesa el resultado en el hilo grafico, guarda la sesion y muestra la ventana del rol o el error.
            protected void done() {
                btnIngresar.setEnabled(true); txtClave.setText("");
                try {
                    Autenticacion.Usuario u=get();
                    if(u==null) {estado.setText("Credenciales incorrectas"); JOptionPane.showMessageDialog(LoginFrame.this,"Usuario o contraseña incorrectos."); return;}
                    Sesion.iniciar(u);
                    JFrame siguiente="Administrador".equals(u.rol)?new AdministradorFrame():"Encuestador".equals(u.rol)?new EncuestadorFrame():"Supervisor".equals(u.rol)?new SupervisorFrame():new RecuentistaFrame();
                    siguiente.setVisible(true); dispose();
                } catch(Exception ex) {
                    estado.setText("No fue posible ingresar");
                    Throwable causa=ex.getCause()==null?ex:ex.getCause();
                    Conexion.mostrarError(LoginFrame.this,causa instanceof Exception?(Exception)causa:ex);
                }
            }
        }.execute();
    }
    // DOCUMENTACION: Inicializa el estilo y crea la ventana de ingreso en el hilo de eventos de Swing.
    public static void main(String[] args) {
        Estilo.iniciar();
        SwingUtilities.invokeLater(()->new LoginFrame().setVisible(true));
    }
}
