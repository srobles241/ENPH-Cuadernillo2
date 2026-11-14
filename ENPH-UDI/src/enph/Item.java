package enph;

/** Elemento de un JComboBox: guarda el id de la BD y el texto visible. */
public class Item {
    public final int id;
    public final String texto;
    public final String extra;

    // DOCUMENTACION: Inicializa el identificador y texto del elemento; la sobrecarga permite incluir un dato extra.
    public Item(int id, String texto) {
        this(id, texto, "");
    }

    // DOCUMENTACION: Inicializa el identificador y texto del elemento; la sobrecarga permite incluir un dato extra.
    public Item(int id, String texto, String extra) {
        this.id = id;
        this.texto = texto;
        this.extra = extra == null ? "" : extra;
    }

    @Override
    // DOCUMENTACION: Devuelve el texto que el JComboBox muestra al usuario.
    public String toString() {
        return texto;
    }
}
