-- ENPH: acceso administrado. Ejecutar con el propietario, tras respaldar.
-- Crea objetos nuevos. No borra perfiles ni cambia los datos existentes.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF
SHOW USER
DECLARE
 n NUMBER;
BEGIN
 SELECT COUNT(*) INTO n FROM user_tables WHERE table_name IN
 ('CASO','CATEGORIA','ENCUESTADOR','GASTO','HOGAR','MIEMBRO','PRODUCTO','RECUENTISTA','SUPERVISOR','VISITA');
 IF n<>10 THEN RAISE_APPLICATION_ERROR(-20101,'Conectate al propietario del esquema ENPH.'); END IF;
 SELECT COUNT(*) INTO n FROM user_tab_columns WHERE table_name='GASTO' AND column_name='UNIDAD_MEDIDA';
 IF n<>1 THEN RAISE_APPLICATION_ERROR(-20102,'Primero aplica la migracion previa de unidad por gasto.'); END IF;
 SELECT COUNT(*) INTO n FROM user_objects WHERE object_name IN ('ENPH_USUARIO','ENPH_AUD_ADMIN','ENPH_SEQ_AUD_ADMIN') OR object_name LIKE 'ENPHA\_AUD\_%' ESCAPE '\';
 IF n>0 THEN RAISE_APPLICATION_ERROR(-20103,'Hay objetos de esta migracion: revisa USER_OBJECTS antes de repetirla.'); END IF;
END;
/
CREATE TABLE enph_usuario (
 login VARCHAR2(50) CONSTRAINT enpha_usuario_pk PRIMARY KEY,
 nombre VARCHAR2(100) NOT NULL,
 rol VARCHAR2(15) NOT NULL,
 clave_hash VARCHAR2(255) NOT NULL,
 activo CHAR(1) DEFAULT 'S' NOT NULL,
 id_encuestador NUMBER(10) UNIQUE REFERENCES encuestador(id_encuestador),
 id_recuentista NUMBER(10) UNIQUE REFERENCES recuentista(id_recuentista),
 id_supervisor NUMBER(10) UNIQUE REFERENCES supervisor(id_supervisor),
 CONSTRAINT enpha_usuario_activo CHECK (activo IN ('S','N')),
 CONSTRAINT enpha_usuario_login CHECK (login=LOWER(login) AND login=TRIM(login)),
 CONSTRAINT enpha_usuario_rol CHECK (
  (rol='Administrador' AND login='administrador' AND activo='S' AND id_encuestador IS NULL AND id_recuentista IS NULL AND id_supervisor IS NULL)
  OR (login<>'administrador' AND (
   (rol='Encuestador' AND id_encuestador IS NOT NULL AND id_recuentista IS NULL AND id_supervisor IS NULL)
   OR (rol='Recuentista' AND id_encuestador IS NULL AND id_recuentista IS NOT NULL AND id_supervisor IS NULL)
   OR (rol='Supervisor' AND id_encuestador IS NULL AND id_recuentista IS NULL AND id_supervisor IS NOT NULL)
  ))
 )
);
CREATE TABLE enph_aud_admin (
 id_aud NUMBER PRIMARY KEY,
 fecha DATE DEFAULT SYSDATE NOT NULL,
 tabla_objeto VARCHAR2(30) NOT NULL,
 id_registro VARCHAR2(110) NOT NULL,
 accion VARCHAR2(20) NOT NULL,
 contexto VARCHAR2(64) NOT NULL,
 marca VARCHAR2(40) NOT NULL
);
CREATE SEQUENCE enph_seq_aud_admin;

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

CREATE OR REPLACE TRIGGER enpha_aud_hogar
AFTER INSERT OR UPDATE OR DELETE ON HOGAR FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_HOGAR);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_HOGAR);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_HOGAR);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'HOGAR',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_hogar

CREATE OR REPLACE TRIGGER enpha_aud_visita
AFTER INSERT OR UPDATE OR DELETE ON VISITA FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_VISITA);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_VISITA);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_VISITA);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'VISITA',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_visita

CREATE OR REPLACE TRIGGER enpha_aud_gasto
AFTER INSERT OR UPDATE OR DELETE ON GASTO FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_GASTO);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_GASTO);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_GASTO);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'GASTO',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_gasto

CREATE OR REPLACE TRIGGER enpha_aud_caso
AFTER INSERT OR UPDATE OR DELETE ON CASO FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_CASO);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_CASO);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_CASO);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'CASO',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_caso

CREATE OR REPLACE TRIGGER enpha_aud_categoria
AFTER INSERT OR UPDATE OR DELETE ON CATEGORIA FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_CATEGORIA);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_CATEGORIA);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_CATEGORIA);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'CATEGORIA',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_categoria

CREATE OR REPLACE TRIGGER enpha_aud_producto
AFTER INSERT OR UPDATE OR DELETE ON PRODUCTO FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_PRODUCTO);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_PRODUCTO);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_PRODUCTO);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'PRODUCTO',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_producto

CREATE OR REPLACE TRIGGER enpha_aud_encuestador
AFTER INSERT OR UPDATE OR DELETE ON ENCUESTADOR FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_ENCUESTADOR);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_ENCUESTADOR);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_ENCUESTADOR);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'ENCUESTADOR',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_encuestador

CREATE OR REPLACE TRIGGER enpha_aud_recuentista
AFTER INSERT OR UPDATE OR DELETE ON RECUENTISTA FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_RECUENTISTA);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_RECUENTISTA);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_RECUENTISTA);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'RECUENTISTA',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_recuentista

CREATE OR REPLACE TRIGGER enpha_aud_supervisor
AFTER INSERT OR UPDATE OR DELETE ON SUPERVISOR FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_SUPERVISOR);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_SUPERVISOR);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_SUPERVISOR);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'SUPERVISOR',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_supervisor

CREATE OR REPLACE TRIGGER enpha_aud_miembro
AFTER INSERT OR UPDATE OR DELETE ON MIEMBRO FOR EACH ROW
DECLARE
 v_contexto VARCHAR2(64):=SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER');
 v_accion VARCHAR2(20);
 v_registro VARCHAR2(110);
BEGIN
 IF SUBSTR(v_contexto,1,8)='ENPHADM|' THEN
  IF INSERTING THEN v_accion:='CREAR';v_registro:=TO_CHAR(:NEW.ID_MIEMBRO);
  ELSIF DELETING THEN v_accion:='ELIMINAR';v_registro:=TO_CHAR(:OLD.ID_MIEMBRO);
  ELSE v_accion:='EDITAR';v_registro:=TO_CHAR(:NEW.ID_MIEMBRO);
  
  END IF;
  INSERT INTO enph_aud_admin(id_aud,tabla_objeto,id_registro,accion,contexto,marca)
  VALUES(enph_seq_aud_admin.NEXTVAL,'MIEMBRO',v_registro,v_accion,v_contexto,'Editado por el administrador');
 END IF;
END;
/
SHOW ERRORS TRIGGER enpha_aud_miembro

DECLARE n NUMBER;
BEGIN
 SELECT COUNT(*) INTO n FROM user_objects
 WHERE object_type='TRIGGER' AND object_name LIKE 'ENPHA\_AUD\_%' ESCAPE '\' AND status='VALID';
 IF n<>11 THEN RAISE_APPLICATION_ERROR(-20104,'Faltan disparadores validos; revisa USER_ERRORS.'); END IF;
END;
/
PROMPT Migracion creada. Ejecuta InicializarAdministrador.java una sola vez.
SET DEFINE ON
