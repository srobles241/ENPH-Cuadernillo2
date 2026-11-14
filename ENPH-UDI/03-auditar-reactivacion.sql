-- Solo en esquemas que ya tienen instalada la migracion 01 anterior.
WHENEVER SQLERROR EXIT SQL.SQLCODE
CREATE OR REPLACE TRIGGER enpha_aud_enph_usuario
AFTER INSERT OR UPDATE OR DELETE ON ENPH_USUARIO FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=:NEW.login;
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=:OLD.login;
  ELSE v_accion:='EDITAR';v_registro:=:NEW.login;
  IF :OLD.activo='S' AND :NEW.activo='N' THEN v_accion:='DESACTIVAR';
  ELSIF :OLD.activo='N' AND :NEW.activo='S' THEN v_accion:='REACTIVAR'; END IF;
  IF :OLD.login<>:NEW.login THEN v_registro:=:OLD.login||' -> '||:NEW.login; END IF;
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'ENPH_USUARIO',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_enph_usuario
