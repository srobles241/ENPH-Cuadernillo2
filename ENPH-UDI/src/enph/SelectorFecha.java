package enph;
import java.awt.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;

/** Calendario completo; el formulario aplica después la regla del ciclo del hogar. */
public class SelectorFecha extends JPanel {
    private final JSpinner fecha=new JSpinner(new SpinnerDateModel());
    // DOCUMENTACION: Construye los componentes de esta clase, configura su estado inicial y registra los eventos definidos en el constructor.
    public SelectorFecha() {
        super(new FlowLayout(FlowLayout.LEFT,6,0));
        fecha.setEditor(new JSpinner.DateEditor(fecha,"dd/MM/yyyy"));
        fecha.setPreferredSize(new Dimension(145,30));
        add(fecha); JButton elegir=new JButton("Calendario");add(elegir);
        elegir.addActionListener(e->abrirCalendario());
    }
    // DOCUMENTACION: Confirma la edicion del campo y convierte la fecha a LocalDate usando la zona del equipo; rechaza formatos invalidos.
    public LocalDate getFecha() {
        try {fecha.commitEdit();}catch(java.text.ParseException e){throw new IllegalArgumentException("Escribe una fecha válida en formato dd/MM/yyyy.");}
        return ((Date)fecha.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    // DOCUMENTACION: Actualiza el selector desde un LocalDate usando el inicio del dia en la zona del equipo.
    public void setFecha(LocalDate valor) {fecha.setValue(Date.from(valor.atStartOfDay(ZoneId.systemDefault()).toInstant()));}
    // DOCUMENTACION: Registra un oyente de cambios del selector de fecha.
    public void addChangeListener(ChangeListener listener){fecha.addChangeListener(listener);}
    // DOCUMENTACION: Abre un dialogo modal con el calendario y lo cierra cuando se selecciona una fecha.
    private void abrirCalendario() {
        final JDialog dialogo=new JDialog(SwingUtilities.getWindowAncestor(this),"Elegir fecha",Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.add(panelCalendario(()->dialogo.dispose()));dialogo.pack();dialogo.setLocationRelativeTo(this);dialogo.setVisible(true);
    }
    // DOCUMENTACION: Construye la navegacion de mes y ano, los botones de dias y Hoy; invoca cerrar al elegir una fecha.
    public JPanel panelCalendario(Runnable cerrar) {
        LocalDate inicial=getFecha();
        JPanel panel=new JPanel(new BorderLayout(8,12));panel.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        JPanel arriba=new JPanel(new FlowLayout());
        JComboBox<String> meses=new JComboBox<>();for(Month m:Month.values())meses.addItem(m.getDisplayName(TextStyle.FULL,new Locale("es","CO")));
        meses.setSelectedIndex(inicial.getMonthValue()-1);
        JSpinner anio=new JSpinner(new SpinnerNumberModel(inicial.getYear(),1,9999,1));anio.setEditor(new JSpinner.NumberEditor(anio,"#"));
        JButton anterior=new JButton("‹"),siguiente=new JButton("›");arriba.add(anterior);arriba.add(meses);arriba.add(anio);arriba.add(siguiente);
        JPanel dias=new JPanel(new GridLayout(0,7,4,4));
        Runnable dibujar=()->{
            dias.removeAll();for(String d:new String[]{"Lun","Mar","Mié","Jue","Vie","Sáb","Dom"})dias.add(new JLabel(d,SwingConstants.CENTER));
            YearMonth mes=YearMonth.of((Integer)anio.getValue(),meses.getSelectedIndex()+1);
            int huecos=mes.atDay(1).getDayOfWeek().getValue()-1;
            for(int i=0;i<huecos;i++)dias.add(new JLabel(""));
            for(int i=1;i<=mes.lengthOfMonth();i++) {
                final LocalDate valor=mes.atDay(i);JButton b=new JButton(String.valueOf(i));
                if(valor.equals(getFecha()))b.setForeground(Estilo.PRIMARIO);
                b.addActionListener(e->{setFecha(valor);cerrar.run();});dias.add(b);
            }
            for(int i=huecos+mes.lengthOfMonth();i<42;i++)dias.add(new JLabel(""));dias.revalidate();dias.repaint();
        };
        meses.addActionListener(e->dibujar.run());anio.addChangeListener(e->dibujar.run());
        anterior.addActionListener(e->{YearMonth m=YearMonth.of((Integer)anio.getValue(),meses.getSelectedIndex()+1).minusMonths(1);if(m.getYear()>=1){anio.setValue(m.getYear());meses.setSelectedIndex(m.getMonthValue()-1);}});
        siguiente.addActionListener(e->{YearMonth m=YearMonth.of((Integer)anio.getValue(),meses.getSelectedIndex()+1).plusMonths(1);if(m.getYear()<=9999){anio.setValue(m.getYear());meses.setSelectedIndex(m.getMonthValue()-1);}});
        JButton hoy=new JButton("Hoy");hoy.addActionListener(e->{setFecha(LocalDate.now());cerrar.run();});
        panel.add(arriba,BorderLayout.NORTH);panel.add(dias);panel.add(hoy,BorderLayout.SOUTH);dibujar.run();return panel;
    }
}
