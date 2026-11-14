# ENPH Cuadernillo 2

Proyecto integrador para registrar y consultar gastos de hogares urbanos durante un ciclo de 14 días. Desarrollado con Java Swing, JDBC y Oracle.

## Versión UDI

El [proyecto de NetBeans](ENPH-UDI/) incluye los cinco reportes y el código compatible con Java 8 o superior.

1. Abre ENPH-UDI como proyecto en NetBeans.
2. Coloca el controlador Oracle ojdbc14.jar en ENPH-UDI/lib.
3. Copia conexion.local.properties.example como conexion.local.properties junto a build.xml y completa los datos de tu entorno.
4. En una base UDI existente, verifica el instalador de funciones y las migraciones 10, 11 y 12 de oracle-udi. Las migraciones 06 y 07 documentan la creación de esquemas y los roles y se aplican si aún faltan.
5. Ejecuta Clean and Build y abre LoginFrame. Para ejecutar el JAR desde la carpeta ENPH-UDI, usa:

   `java -cp "dist\Dise_oProyectoU.jar;lib\ojdbc14.jar" enph.LoginFrame`

El respaldo de Oracle y la configuración con credenciales se mantienen fuera del repositorio.
