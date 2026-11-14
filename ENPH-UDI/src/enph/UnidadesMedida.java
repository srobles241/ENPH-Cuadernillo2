package enph;

import java.text.Normalizer;
import java.util.Locale;
import javax.swing.JComboBox;

/** Opciones fijas para adquisiciones del hogar; no se alimentan desde Oracle. */
final class UnidadesMedida {
    private static final String[] OPCIONES={
        "mg","g","kg","tonelada","onza","libra",
        "ml","cl","dl","litro","galón","taza","cucharada","cucharadita",
        "unidad","pieza","par","media docena","docena","centenar",
        "porción","ración","paquete","bolsa","sobre","caja","botella",
        "lata","frasco","envase","bandeja","bulto","saco","manojo","racimo","rollo",
        "mm","cm","m","km","m²","m³",
        "minuto","hora","día","semana","mes","servicio"
    };
    private UnidadesMedida(){}
    static JComboBox<String> combo(){
        JComboBox<String> c=new JComboBox<>(OPCIONES);
        c.setEditable(false);
        c.setSelectedIndex(-1);
        return c;
    }
    static void seleccionar(JComboBox<String> c,String dato){
        c.setSelectedItem(canonica(dato));
    }
    static String canonica(String dato){
        if(dato==null)return null;
        String n=normal(dato);
        switch(n){
            case "gramo":case "gramos":case "gr":return "g";
            case "kilogramo":case "kilogramos":case "kilo":case "kilos":return "kg";
            case "miligramo":case "miligramos":return "mg";
            case "mililitro":case "mililitros":case "cc":return "ml";
            case "l":case "litros":return "litro";
            case "lb":case "libras":return "libra";
            case "oz":case "onzas":return "onza";
            case "un":case "ud":case "uds":case "unidades":return "unidad";
            case "m2":return "m²";
            case "m3":return "m³";
            default:for(String opcion:OPCIONES)if(normal(opcion).equals(n))return opcion;
        }
        return null;
    }
    private static String normal(String texto){
        return Normalizer.normalize(texto.trim(),Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
    }
}
