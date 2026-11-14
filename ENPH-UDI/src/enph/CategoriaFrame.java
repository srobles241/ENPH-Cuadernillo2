package enph;
import java.awt.*;
import javax.swing.*;

/** Organización de categorías de adquisición. Oracle conserva las restricciones referenciales. */
public class CategoriaFrame extends JFrame {
    private final JTable tabla=Estilo.tabla();
    private final JTextField nombre=new JTextField(25);
    // DOCUMENTACION: Construye los componentes de esta clase, configura su estado inicial y registra los eventos definidos en el constructor.
    public CategoriaFrame() {
        super("ENPH | Categorías de adquisición");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); setSize(720,480); setLocationRelativeTo(null);
        add(Estilo.encabezado("Categorías de adquisición","Organizar los productos de la encuesta"),BorderLayout.NORTH);
        JPanel centro=new JPanel(new BorderLayout(12,12));
        centro.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JPanel form=new JPanel(); form.add(new JLabel("Nombre de categoría")); form.add(nombre);
        centro.add(form,BorderLayout.NORTH); centro.add(new JScrollPane(tabla)); add(centro);
        JButton crear=new JButton("Añadir categoría"),editar=new JButton("Cambiar nombre"),borrar=new JButton("Retirar categoría"),refrescar=new JButton("Ver resultados");
        add(Estilo.botones(crear,editar,borrar,refrescar),BorderLayout.SOUTH);
        tabla.getSelectionModel().addListSelectionListener(e->{if(tabla.getSelectedRow()>=0)nombre.setText(String.valueOf(tabla.getValueAt(tabla.getSelectedRow(),1)));});
        crear.addActionListener(e->guardar(false)); editar.addActionListener(e->guardar(true));
        borrar.addActionListener(e->eliminar()); refrescar.addActionListener(e->cargar()); cargar();
        Estilo.agregarCerrar(this);
    }
    // DOCUMENTACION: Consulta y muestra todas las categorias por identificador.
    private void cargar() {
        try {Estilo.mostrar(tabla,Conexion.consultar("SELECT id_categoria,nombre_categoria FROM categoria ORDER BY id_categoria"),"ID","Categoría");}
        catch(Exception ex){Conexion.mostrarError(this,ex);}
    }
    // DOCUMENTACION: Valida un nombre de 1 a 50 caracteres; inserta o actualiza segun editar y recarga la tabla.
    private void guardar(boolean editar) {
        String n=nombre.getText().trim();
        if(n.isEmpty()||n.length()>50){Conexion.info(this,"El nombre debe tener entre 1 y 50 caracteres.");return;}
        if(editar&&tabla.getSelectedRow()<0){Conexion.info(this,"Selecciona una categoría.");return;}
        try {
            if(editar)Conexion.ejecutarAuditado("CATEGORIA",tabla.getValueAt(tabla.getSelectedRow(),0),"EDITAR","UPDATE categoria SET nombre_categoria=? WHERE id_categoria=?",n,tabla.getValueAt(tabla.getSelectedRow(),0));
            else Conexion.ejecutarAuditado("CATEGORIA",null,"CREAR","INSERT INTO categoria(id_categoria,nombre_categoria) VALUES(seq_categoria.NEXTVAL,?)",n);
            cargar(); nombre.setText("");
        } catch(Exception ex){Conexion.mostrarError(this,ex);}
    }
    // DOCUMENTACION: Solicita confirmacion y elimina la categoria seleccionada; explica ORA-02292 cuando tiene productos asociados.
    private void eliminar() {
        if(tabla.getSelectedRow()<0){Conexion.info(this,"Selecciona una categoría.");return;}
        if(JOptionPane.showConfirmDialog(this,"¿Eliminar la categoría seleccionada?","Confirmar eliminación",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try {Conexion.ejecutarAuditado("CATEGORIA",tabla.getValueAt(tabla.getSelectedRow(),0),"ELIMINAR","DELETE FROM categoria WHERE id_categoria=?",tabla.getValueAt(tabla.getSelectedRow(),0)); cargar(); nombre.setText("");}
        catch(java.sql.SQLException ex){if(ex.getErrorCode()==2292)Conexion.info(this,"Esta categoría tiene productos asociados y no se puede eliminar.");else Conexion.mostrarError(this,ex);}
    }
}
