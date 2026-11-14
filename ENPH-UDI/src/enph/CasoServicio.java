package enph;

import java.sql.*;

/** Transiciones del caso: verifica el estado real y confirma ambos registros juntos. */
public final class CasoServicio {
    private interface Operacion { void ejecutar(Connection cn) throws SQLException; }
    // DOCUMENTACION: Ejecuta la operacion con autocommit desactivado; confirma o revierte ante errores SQL o de ejecucion.
    private static void transaccion(Operacion op) throws SQLException {
        try (Connection cn=Conexion.obtener()) {
            cn.setAutoCommit(false);
            try { op.ejecutar(cn); cn.commit(); }
            catch(SQLException | RuntimeException ex) { cn.rollback(); throw ex; }
        }
    }
    // DOCUMENTACION: Exige que el rol de la sesion coincida con el requerido para la operacion.
    private static void rol(String esperado) throws SQLException {
        if(!esperado.equals(Sesion.rol)) throw new SQLException("Esta acción requiere el rol "+esperado+".");
    }
    // DOCUMENTACION: Ejecuta una consulta escalar parametrizada con un identificador y devuelve su resultado entero.
    private static int numero(Connection c,String sql,int id) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    // DOCUMENTACION: Exige visitas para los 14 dias distintos del ciclo y ninguna visita en estado Revisar; no comprueba que cada visita tenga gastos.
    private static void completo(Connection c,int hogar) throws SQLException {
        int dias=numero(c,"SELECT COUNT(DISTINCT numero_dia_ciclo) FROM visita WHERE id_hogar=? AND numero_dia_ciclo BETWEEN 1 AND 14",hogar);
        if(dias!=14) throw new SQLException("Faltan "+(14-dias)+" días por registrar. El caso requiere 14 días completos.");
        if(numero(c,"SELECT COUNT(*) FROM visita WHERE id_hogar=? AND estado_visita='Revisar'",hogar)>0)
            throw new SQLException("Hay visitas pendientes de revisión.");
    }
    // DOCUMENTACION: Bloquea el hogar con FOR UPDATE, comprueba su existencia y, si corresponde, el encuestador asignado; devuelve el estado.
    private static String bloquearHogar(Connection c,int hogar,int encuestador) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT estado,id_encuestador_asignado FROM hogar WHERE id_hogar=? FOR UPDATE")) {
            p.setInt(1,hogar);try(ResultSet r=p.executeQuery()) {
                if(!r.next())throw new SQLException("El hogar ya no existe.");
                if(encuestador>0 && r.getInt(2)!=encuestador)throw new SQLException("El hogar no está asignado a tu usuario.");
                return r.getString(1);
            }
        }
    }
    // DOCUMENTACION: Bloquea el caso con FOR UPDATE y verifica que pertenezca al hogar indicado; devuelve su estado.
    private static String bloquearCaso(Connection c,int caso,int hogar) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT estado_caso FROM caso WHERE id_caso=? AND id_hogar=? FOR UPDATE")) {
            p.setInt(1,caso);p.setInt(2,hogar);try(ResultSet r=p.executeQuery()) {
                if(!r.next())throw new SQLException("El caso ya no existe o no corresponde al hogar.");
                return r.getString(1);
            }
        }
    }
    // DOCUMENTACION: Permite revisar estados Completo o Con alertas y rechaza los casos ya aprobados.
    private static void revisable(String estado) throws SQLException {
        if("Aprobado".equals(estado)) throw new SQLException("El caso ya está aprobado. La aprobación se conserva.");
        if(!"Completo".equals(estado)&&!"Con alertas".equals(estado))
            throw new SQLException("Solo se pueden revisar casos completos o con alertas. Actualiza la lista.");
    }
    // DOCUMENTACION: Exige Supervisor y delega en Oracle la validacion del ciclo; actualiza caso, hogar y visitas en una misma transaccion.
    public static void aprobar(final int caso,final int hogar) throws SQLException {
        rol("Supervisor"); final int supervisor=Sesion.id;
        transaccion(c->{
            bloquearHogar(c,hogar,0);revisable(bloquearCaso(c,caso,hogar));
            if(numero(c,"SELECT COUNT(*) FROM supervisor WHERE id_supervisor=?",supervisor)!=1)
                throw new SQLException("Supervisor no válido.");
            try(PreparedStatement p=c.prepareStatement("UPDATE caso SET estado_caso='Aprobado',id_supervisor_aprobador=? WHERE id_caso=?")) {
                p.setInt(1,supervisor);p.setInt(2,caso);p.executeUpdate();
                Auditoria.registrar(c,"CASO",caso,"EDITAR");
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE hogar SET estado='Completo',fecha_cierre=NVL(fecha_cierre,SYSDATE) WHERE id_hogar=?")) {
                p.setInt(1,hogar);p.executeUpdate();
                Auditoria.registrar(c,"HOGAR",hogar,"EDITAR");
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE visita SET estado_visita='Completa',id_supervisor_aprobador=? WHERE id_hogar=?")) {
                p.setInt(1,supervisor);p.setInt(2,hogar);p.executeUpdate();
            }
            Auditoria.visitasDelHogar(c,hogar);
        });
    }
    // DOCUMENTACION: Exige Supervisor y un caso revisable; marca el caso Devuelto y reabre el hogar como Asignado.
    public static void devolver(final int caso,final int hogar) throws SQLException {
        rol("Supervisor"); final int supervisor=Sesion.id;
        transaccion(c->{
            bloquearHogar(c,hogar,0);revisable(bloquearCaso(c,caso,hogar));
            try(PreparedStatement p=c.prepareStatement("UPDATE caso SET estado_caso='Devuelto',id_supervisor_aprobador=? WHERE id_caso=?")) {
                p.setInt(1,supervisor);p.setInt(2,caso);p.executeUpdate();
                Auditoria.registrar(c,"CASO",caso,"EDITAR");
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE hogar SET estado='Asignado',fecha_cierre=NULL WHERE id_hogar=?")) {
                p.setInt(1,hogar);p.executeUpdate();
                Auditoria.registrar(c,"HOGAR",hogar,"EDITAR");
            }
        });
    }
    // DOCUMENTACION: Exige Encuestador y hogar asignado al usuario; valida el ciclo y crea o actualiza el caso como Completo.
    public static void cerrar(final int hogar) throws SQLException {
        rol("Encuestador"); final int encuestador=Sesion.id;
        transaccion(c->{
            String estado= bloquearHogar(c,hogar,encuestador);
            int caso=numero(c,"SELECT NVL(MAX(id_caso),0) FROM caso WHERE id_hogar=?",hogar);
            if(caso>0 && "Aprobado".equals(bloquearCaso(c,caso,hogar)))
                throw new SQLException("El caso está aprobado y no puede volver a cerrarse ni modificarse.");
            if(!"Asignado".equals(estado))throw new SQLException("El hogar ya está cerrado. Actualiza la lista.");
            completo(c,hogar);
            try(PreparedStatement p=c.prepareStatement("UPDATE hogar SET estado='Completo',fecha_cierre=SYSDATE WHERE id_hogar=?")) {p.setInt(1,hogar);p.executeUpdate();}
            Auditoria.registrar(c,"HOGAR",hogar,"EDITAR");
            if(caso>0) {
                Auditoria.registrar(c,"CASO",caso,"EDITAR");
                try(PreparedStatement p=c.prepareStatement("UPDATE caso SET estado_caso='Completo',id_supervisor_aprobador=NULL WHERE id_caso=?")) {p.setInt(1,caso);p.executeUpdate();}
            } else {
                try(PreparedStatement p=c.prepareStatement("INSERT INTO caso(id_caso,id_hogar,id_encuestador,estado_caso) VALUES (seq_caso.NEXTVAL,?,?,'Completo')")) {p.setInt(1,hogar);p.setInt(2,encuestador);p.executeUpdate();}
                Auditoria.alta(c,"CASO");
            }
        });
    }
}
