package enph;
import java.sql.*;
import java.time.LocalDate;

/** Resuelve una fecha calendario a una visita única dentro del ciclo de 14 días. */
public final class VisitaServicio {
    /** Oracle valida que la fecha pertenece al ciclo. */
    public static int diaDelCiclo(LocalDate inicio,LocalDate fecha) throws SQLException {
        try(Connection c=Conexion.obtener()){return diaDelCiclo(c,inicio,fecha);}
    }
    private static int diaDelCiclo(Connection c,LocalDate inicio,LocalDate fecha)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT enph3_dia_ciclo(?,?) FROM dual")){
            p.setDate(1,inicio==null?null:Date.valueOf(inicio));
            p.setDate(2,fecha==null?null:Date.valueOf(fecha));
            try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    // DOCUMENTACION: Exige Encuestador y bloquea el hogar asignado. Reutiliza la visita del dia o la inserta, actualiza el avance y devuelve su identificador en una transaccion.
    public static int obtenerOCrear(int hogar,LocalDate fecha) throws SQLException {
        if(!"Encuestador".equals(Sesion.rol))throw new SQLException("Solo el encuestador puede registrar visitas.");
        try(Connection c=Conexion.obtener()) {
            c.setAutoCommit(false);
            try {
                int dia;
                try(PreparedStatement p=c.prepareStatement("SELECT fecha_inicio_ciclo,estado,id_encuestador_asignado FROM hogar WHERE id_hogar=? FOR UPDATE")) {
                    p.setInt(1,hogar);try(ResultSet r=p.executeQuery()) {
                        if(!r.next())throw new SQLException("No existe el hogar.");
                        if(!"Asignado".equals(r.getString(2))||r.getInt(3)!=Sesion.id)throw new SQLException("El hogar está cerrado o no está asignado a tu usuario.");
                        dia=diaDelCiclo(c,r.getDate(1).toLocalDate(),fecha);
                    }
                }
                try(PreparedStatement p=c.prepareStatement("SELECT id_visita FROM visita WHERE id_hogar=? AND numero_dia_ciclo=?")) {
                    p.setInt(1,hogar);p.setInt(2,dia);try(ResultSet r=p.executeQuery()) {if(r.next()){int id=r.getInt(1);c.commit();return id;}}
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO visita(id_visita,id_hogar,id_encuestador,fecha_visita,numero_dia_ciclo,estado_visita) VALUES (seq_visita.NEXTVAL,?,?,?,?,'Al dia')")) {
                    p.setInt(1,hogar);p.setInt(2,Sesion.id);p.setDate(3,Date.valueOf(fecha));p.setInt(4,dia);p.executeUpdate();
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE hogar SET dia_ciclo_actual=GREATEST(dia_ciclo_actual,?) WHERE id_hogar=?")) {p.setInt(1,dia);p.setInt(2,hogar);p.executeUpdate();}
                int id;
                try(PreparedStatement p=c.prepareStatement("SELECT id_visita FROM visita WHERE id_hogar=? AND numero_dia_ciclo=?")) {p.setInt(1,hogar);p.setInt(2,dia);try(ResultSet r=p.executeQuery()){r.next();id=r.getInt(1);}}
                Auditoria.registrar(c,"VISITA",id,"CREAR");
                Auditoria.registrar(c,"HOGAR",hogar,"EDITAR");
                c.commit();return id;
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }
}
