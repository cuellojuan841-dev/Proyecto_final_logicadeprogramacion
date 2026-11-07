# Notas de un curso

Aplicación web para que docentes consulten estudiantes por asignatura, registren sus notas y revisen promedios. Cada estudiante puede consultar su historial académico y la proyección de sus notas pendientes.

## Autor

- Nombre: Juan Andres Cuello Vargas
- Fecha: 7 de noviembre de 2026
- Proyecto: Proyecto 4 — Notas de un curso

## Tecnologías

- Java 21
- Spring Boot 4.1.2
- Vaadin 25.3.4
- Maven Wrapper
- Archivos de texto para guardar los datos

## Requisitos para ejecutar

- JDK 21 instalado.
- Conexión a Internet la primera vez, para que Maven descargue dependencias.

## Cómo ejecutar

Abre PowerShell en la carpeta `notascurso` y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación se inicia localmente. La dirección usual de Spring Boot es `http://localhost:8080`.

Para detenerla, vuelve a la terminal y presiona `Ctrl+C`.

## Cómo ejecutar las pruebas

Desde PowerShell, en la carpeta `notascurso`, ejecuta:

```powershell
.\mvnw.cmd test
```

Las pruebas comprueban cálculos, almacenamiento y validación del formato de usuarios.

## Acceso de prueba

Las cuentas de demostración están en `src/main/resources/usuarios.txt`. Por ejemplo:

| Rol | Usuario | Contraseña |
| Profesor | `profesor1` | `1234` |
| Estudiante | `estudiante1` | `1234` |

Son credenciales locales de demostración; no uses contraseñas reales.

## Funciones

### Profesor

- Muestra las asignaturas asignadas a su cuenta.
- Presenta estudiantes inscritos en la asignatura elegida.
- Permite buscar por nombre y filtrar por estado del promedio.
- Permite ingresar, actualizar y borrar notas desde diálogos; borrar requiere confirmación.
- Muestra notas de tres períodos, promedio actual, promedio general y estado de riesgo.
- Presenta el promedio del grupo, la nota más alta, la más baja y el porcentaje de aprobación.

### Estudiante

- Muestra su historial agrupado por asignatura.
- Presenta la nota de cada período y el porcentaje que representa.
- Proyecta la nota necesaria para alcanzar 3.0, o informa si ya ganó o si ya no le alcanza.

### Navegación

El menú compartido se construye con `AppLayout`. La opción “Notas” lleva a la vista correspondiente al rol. “Salir” limpia el usuario de la sesión y vuelve al inicio de sesión. La vista de Contactos le permitira al estudiante mandarle un mensaje a algun profesor .

## Cálculo de notas

Cada asignatura usa tres períodos con estos pesos:

- Período 1: 30 %
- Período 2: 30 %
- Período 3: 40 %

Las notas van de 0.0 a 5.0 y se validan para aceptar máximo un decimal. El promedio actual se calcula con los períodos que ya tienen nota, normalizando por el peso ya calificado. El estudiante está en riesgo si su promedio actual es menor que 3.0.

## Archivos de datos

### `src/main/resources/usuarios.txt`

Cada línea contiene cinco campos separados por `|`:

```text
usuario|contraseña|nombre|rol|asignatura1,asignatura2
```

Los roles válidos son `PROFESOR` y `ESTUDIANTE`. La lista de asignaturas de cada cuenta se separa con comas. Si una línea está mal formada, el mensaje indica su número de línea.

### `notas.txt`

Se encuentra en la carpeta del proyecto y la aplicación lo crea al guardar la primera nota. Cada línea tiene este formato:

```text
usuarioEstudiante|asignatura|periodo|nota
```

Si todavía no existen notas, el reporte indicará que no hay calificaciones registradas.

> Las contraseñas del archivo de usuarios están en texto plano porque son cuentas de demostración para el proyecto; este formato no debe usarse para cuentas reales.

## Estructura principal

- `MainView.java`: inicio de sesión y navegación según el rol.
- `DashboardLayout.java`: menú y navegación compartida.
- `ProfesorView.java`: registro, edición, consulta y reporte de notas.
- `EstudianteView.java`: historial y proyección académica.
- `cuentaUsuario.java`: datos de usuario y asignaturas asociadas.
- `Nota.java`: datos de una nota.
- `servicio.java`: lectura de usuarios y sesión actual.
- `NotasService.java`: lectura, escritura, validación y cálculos de notas.
- `ContactosView.java`: pantalla de ejemplo para la sección Contactos.

## Capturas de pantalla

Estas capturas son de ejemplo y muestran las vistas principales del programa.

### Inicio de sesión

![Inicio de sesión](docs/capturas/login.png)

### Vista del profesor

![Vista del profesor](docs/capturas/profesor.png)

### Notas del grupo

![Reporte y tabla de notas](docs/capturas/notas-del-grupo.png)

### Vista del estudiante

![Historial académico del estudiante](docs/capturas/estudiante.png)

## Publicación y entrega

Subir el proyecto a un repositorio público de GitHub, incluir los cambios finales y entregar el enlace en la tarea de Teams según las indicaciones del curso.
