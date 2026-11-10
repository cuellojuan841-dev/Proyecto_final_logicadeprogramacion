package com.example.notascurso;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class NotasService {
    private final Path archivo;
    private final Path archivoPlanes;

    public NotasService() {
        this(rutaArchivoPorDefecto());
    }

    public NotasService(Path archivo) {
        this.archivo = archivo;
        this.archivoPlanes = archivo.resolveSibling("planes.txt");
    }

    public Asignatura obtenerAsignatura(String nombre) throws IOException {
        // Creo el objeto de materia y le cargo las actividades configuradas o el plan inicial.
        Asignatura asignatura = new Asignatura(nombre);
        asignatura.setActividades(actividadesDeAsignatura(nombre));
        return asignatura;
    }

    public List<ActividadEvaluativa> actividadesDeAsignatura(String asignatura)
            throws IOException {
        validarTexto(asignatura, "La asignatura");
        List<ActividadEvaluativa> actividades = new ArrayList<>();

        // Si hay planes guardados, busco las filas que correspondan a esta materia.
        if (Files.exists(archivoPlanes)) {
            List<String> lineas = Files.readAllLines(archivoPlanes, StandardCharsets.UTF_8);
            for (int indice = 0; indice < lineas.size(); indice++) {
                String linea = lineas.get(indice).trim();
                // Ignoro líneas vacías para que no se interpreten como actividades.
                if (linea.isEmpty()) {
                    continue;
                }

                // En planes.txt cada fila tiene asignatura, actividad y porcentaje.
                String[] datos = linea.split("\\|", -1);
                if (datos.length != 3) {
                    throw new IOException("Formato inválido en planes.txt, línea "
                            + (indice + 1));
                }

                if (datos[0].trim().equals(asignatura)) {
                    try {
                        // Convierto el porcentaje y valido los datos antes de crear la actividad.
                        String nombre = datos[1].trim();
                        double porcentaje = Double.parseDouble(datos[2].trim());
                        validarActividad(nombre, porcentaje);
                        actividades.add(new ActividadEvaluativa(nombre, porcentaje));
                    } catch (IllegalArgumentException e) {
                        // Informo qué fila del archivo tiene un valor que no se puede usar.
                        throw new IOException("Dato inválido en planes.txt, línea "
                                + (indice + 1), e);
                    }
                }
            }
        }

        // Si no había plan para esta materia, uso el reparto inicial de tres períodos.
        if (actividades.isEmpty()) {
            actividades.add(new ActividadEvaluativa("Período 1", 30));
            actividades.add(new ActividadEvaluativa("Período 2", 30));
            actividades.add(new ActividadEvaluativa("Período 3", 40));
        }
        return actividades;
    }

    public void guardarPlanEvaluacion(String asignatura,
            List<ActividadEvaluativa> actividades) throws IOException {
        // Antes de escribir, verifico que el plan sea válido y no deje notas sin actividad.
        validarTexto(asignatura, "La asignatura");
        if (actividades == null || actividades.isEmpty()) {
            throw new IllegalArgumentException("El plan debe tener al menos una actividad.");
        }

        double suma = 0;
        // Reviso cada actividad, sumo sus pesos y comparo nombres para evitar repetidos.
        for (int indice = 0; indice < actividades.size(); indice++) {
            ActividadEvaluativa actividad = actividades.get(indice);
            validarActividad(actividad.getNombre(), actividad.getPorcentaje());
            suma += actividad.getPorcentaje();

            // Comparo con las actividades anteriores, no conmigo misma.
            for (int anterior = 0; anterior < indice; anterior++) {
                if (actividades.get(anterior).getNombre()
                        .equalsIgnoreCase(actividad.getNombre().trim())) {
                    throw new IllegalArgumentException(
                            "No repitas el nombre de una actividad.");
                }
            }
        }

        // Los pesos del plan deben completar el 100%.
        if (Math.abs(suma - 100.0) > 1e-8) {
            throw new IllegalArgumentException(
                    "Los porcentajes de las actividades deben sumar exactamente 100%.");
        }

        List<Nota> notas = leerTodas();
        // No dejo borrar posiciones del plan que todavía tengan notas asociadas.
        for (Nota nota : notas) {
            if (nota.getAsignatura().equals(asignatura)
                    && nota.getPeriodo() > actividades.size()) {
                throw new IllegalArgumentException(
                        "No puedes quitar una actividad que ya tiene notas guardadas.");
            }
        }

        List<String> lineas = new ArrayList<>();
        if (Files.exists(archivoPlanes)) {
            List<String> actuales = Files.readAllLines(archivoPlanes, StandardCharsets.UTF_8);
            // Conservo las filas de otras asignaturas y reemplazo solo esta materia.
            for (int indice = 0; indice < actuales.size(); indice++) {
                String linea = actuales.get(indice);
                if (linea.isBlank()) {
                    continue;
                }
                String[] datos = linea.split("\\|", -1);
                if (datos.length != 3) {
                    throw new IOException("Formato inválido en planes.txt, línea "
                            + (indice + 1));
                }
                if (!datos[0].trim().equals(asignatura)) {
                    lineas.add(linea);
                }
            }
        }

        // Agrego al archivo las actividades nuevas respetando su orden.
        for (ActividadEvaluativa actividad : actividades) {
            lineas.add(asignatura + "|" + actividad.getNombre().trim()
                    + "|" + actividad.getPorcentaje());
        }

        Path carpeta = archivoPlanes.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        // Creo planes.txt si falta o reescribo la lista completa con el plan actualizado.
        Files.write(archivoPlanes, lineas, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public List<Nota> obtenerNotas(String asignatura) throws IOException {
        // Este método sirve para obtener todas las notas asociadas a una sola materia.
        List<Nota> resultado = new ArrayList<>();
        // Filtro todas las notas y guardo solo las de la materia solicitada.
        for (Nota nota : leerTodas()) {
            if (nota.getAsignatura().equals(asignatura)) {
                resultado.add(nota);
            }
        }
        return resultado;
    }

    public List<Nota> obtenerNotasEstudiante(String usuario) throws IOException {
        // Este método reúne las notas de una persona, sin mezclar las de otros estudiantes.
        List<Nota> resultado = new ArrayList<>();
        // Devuelvo únicamente las notas que pertenecen a este estudiante.
        for (Nota nota : leerTodas()) {
            if (nota.getUsuarioEstudiante().equals(usuario)) {
                resultado.add(nota);
            }
        }
        return resultado;
    }

    public Double obtenerNota(String usuario, String asignatura, int periodo) throws IOException {
        // La búsqueda usa los tres datos que identifican una nota dentro del archivo.
        // Busco la nota exacta; null significa que esa actividad sigue pendiente.
        for (Nota nota : leerTodas()) {
            if (coincide(nota, usuario, asignatura, periodo)) {
                return nota.getValor();
            }
        }
        return null;
    }

    public void guardarNota(String usuario, String asignatura, int periodo, double valor)
            throws IOException {
        // Valido primero los datos recibidos para no escribir notas fuera del formato esperado.
        validarTexto(usuario, "El usuario del estudiante");
        validarTexto(asignatura, "La asignatura");
        validarNota(valor);

        List<ActividadEvaluativa> actividades = actividadesDeAsignatura(asignatura);
        // Confirmo que el número elegido exista dentro del plan actual.
        if (periodo < 1 || periodo > actividades.size()) {
            throw new IllegalArgumentException(
                    "Selecciona una actividad que exista en el plan de evaluación.");
        }

        List<Nota> notas = leerTodas();
        List<Nota> notasActualizadas = new ArrayList<>();
        // Copio las otras notas, pero no la anterior de esta misma actividad.
        for (Nota nota : notas) {
            if (!coincide(nota, usuario, asignatura, periodo)) {
                notasActualizadas.add(nota);
            }
        }
        // Agrego la nota nueva y guardo la lista completa.
        notasActualizadas.add(new Nota(usuario, asignatura, periodo, valor));
        guardarTodas(notasActualizadas);
    }

    public boolean eliminarNota(String usuario, String asignatura, int periodo) throws IOException {
        // El resultado indica si encontré y quité una nota; si no existe, queda en false.
        if (periodo < 1) {
            throw new IllegalArgumentException("La actividad debe ser un número positivo.");
        }
        List<Nota> notas = leerTodas();
        boolean eliminada = false;
        // Recorro desde el final porque voy quitando elementos de la lista.
        for (int indice = notas.size() - 1; indice >= 0; indice--) {
            Nota nota = notas.get(indice);
            if (coincide(nota, usuario, asignatura, periodo)) {
                notas.remove(indice);
                eliminada = true;
            }
        }
        // Solo reescribo el archivo si encontré algo para eliminar.
        if (eliminada) {
            guardarTodas(notas);
        }
        return eliminada;
    }

    public Double promedioActual(String usuario, String asignatura) throws IOException {
        // Calculo el promedio solo sobre el porcentaje ya calificado, no sobre lo pendiente.
        List<Nota> notas = notasDeEstudianteYMateria(leerTodas(), usuario, asignatura);
        double puntos = 0;
        double pesoCalificado = 0;

        // Sumo nota por peso y también los pesos de las actividades calificadas.
        for (Nota nota : notas) {
            double peso = pesoActividad(asignatura, nota.getPeriodo());
            puntos += nota.getValor() * peso;
            pesoCalificado += peso;
        }

        // Sin peso calificado todavía no se puede calcular el promedio.
        if (pesoCalificado == 0) {
            return null;
        }
        return puntos / pesoCalificado;
    }

    public double puntosAcumulados(String usuario, String asignatura) throws IOException {
        double puntos = 0;
        // Este total no se divide: representa el aporte real hacia la nota final.
        for (Nota nota : notasDeEstudianteYMateria(leerTodas(), usuario, asignatura)) {
            puntos += nota.getValor() * pesoActividad(asignatura, nota.getPeriodo());
        }
        return puntos;
    }

    public double porcentajePendiente(String usuario, String asignatura) throws IOException {
        // Devuelvo como fracción (por ejemplo 0.30) el peso de las actividades sin nota.
        List<ActividadEvaluativa> actividades = actividadesDeAsignatura(asignatura);
        List<Nota> notas = notasDeEstudianteYMateria(leerTodas(), usuario, asignatura);
        double pendiente = 0;

        // Reviso cada actividad y busco si ya hay una nota para su número.
        for (int indice = 0; indice < actividades.size(); indice++) {
            boolean calificada = false;
            for (Nota nota : notas) {
                if (nota.getPeriodo() == indice + 1) {
                    calificada = true;
                }
            }
            // Si no encontré nota, agrego el porcentaje al peso pendiente.
            if (!calificada) {
                pendiente += actividades.get(indice).getPorcentaje() / 100.0;
            }
        }
        return pendiente;
    }

    private double pesoActividad(String asignatura, int numeroActividad) throws IOException {
        List<ActividadEvaluativa> actividades = actividadesDeAsignatura(asignatura);
        // Si una nota apunta a una actividad inexistente, el archivo quedó inconsistente.
        if (numeroActividad < 1 || numeroActividad > actividades.size()) {
            throw new IOException("La nota guardada hace referencia a una actividad "
                    + "que no existe en el plan de " + asignatura + ".");
        }
        return actividades.get(numeroActividad - 1).getPorcentaje() / 100.0;
    }

    private List<Nota> leerTodas() throws IOException {
        // Este método centraliza la lectura y validación del archivo para los demás cálculos.
        // Es normal que todavía no exista el archivo antes de guardar la primera nota.
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }

        List<Nota> notas = new ArrayList<>();
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        // Cada línea se convierte en un objeto Nota después de revisar sus campos.
        for (int indice = 0; indice < lineas.size(); indice++) {
            String linea = lineas.get(indice).trim();
            if (linea.isEmpty()) {
                continue;
            }

            String[] datos = linea.split("\\|", -1);
            // Una nota debe traer usuario, materia, actividad y valor.
            if (datos.length != 4) {
                throw new IOException("Formato inválido en notas.txt, línea " + (indice + 1));
            }

            try {
                // Convierto los números y valido los datos antes de guardar la nota en la lista.
                String usuario = datos[0].trim();
                String materia = datos[1].trim();
                int periodo = Integer.parseInt(datos[2].trim());
                double valor = Double.parseDouble(datos[3].trim());
                validarTexto(usuario, "El usuario del estudiante");
                validarTexto(materia, "La asignatura");
                if (periodo < 1) {
                    throw new IllegalArgumentException(
                            "El número de actividad debe ser positivo.");
                }
                validarNota(valor);
                notas.add(new Nota(usuario, materia, periodo, valor));
            } catch (IllegalArgumentException e) {
                // Cambio el error de dato por uno que también indique la línea del archivo.
                throw new IOException("Dato inválido en notas.txt, línea " + (indice + 1), e);
            }
        }
        return notas;
    }

    private void guardarTodas(List<Nota> notas) throws IOException {
        // Recibo la lista completa y la convierto al formato de texto de notas.txt.
        Path carpeta = archivo.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }

        List<String> lineas = new ArrayList<>();
        // Paso cada objeto Nota al formato de texto que usamos en el archivo.
        for (Nota nota : notas) {
            lineas.add(nota.getUsuarioEstudiante() + "|"
                    + nota.getAsignatura() + "|"
                    + nota.getPeriodo() + "|"
                    + nota.getValor());
        }

        // Reemplazo el archivo con la versión actualizada de todas las notas.
        Files.write(archivo, lineas, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static List<Nota> notasDeEstudianteYMateria(
            List<Nota> notas, String usuario, String asignatura) {
        List<Nota> resultado = new ArrayList<>();
        // Una nota entra al resultado solo si coincide la persona y la materia.
        for (Nota nota : notas) {
            if (nota.getUsuarioEstudiante().equals(usuario)
                    && nota.getAsignatura().equals(asignatura)) {
                resultado.add(nota);
            }
        }
        return resultado;
    }

    private static boolean coincide(Nota nota, String usuario, String asignatura, int periodo) {
        // Dos notas coinciden solo si pertenecen a la misma persona, materia y actividad.
        return nota.getUsuarioEstudiante().equals(usuario)
                && nota.getAsignatura().equals(asignatura)
                && nota.getPeriodo() == periodo;
    }

    private static void validarActividad(String nombre, double porcentaje) {
        validarTexto(nombre, "El nombre de la actividad");
        // El nombre no puede meter una nueva línea dentro del archivo.
        if (nombre.contains("\n") || nombre.contains("\r")) {
            throw new IllegalArgumentException("El nombre no puede tener saltos de línea.");
        }
        // Cada actividad debe tener un porcentaje válido dentro del rango permitido.
        if (!Double.isFinite(porcentaje) || porcentaje <= 0 || porcentaje > 100) {
            throw new IllegalArgumentException(
                    "Cada porcentaje debe ser mayor que 0 y máximo 100.");
        }
    }

    private static void validarTexto(String valor, String nombre) {
        // El símbolo | separa campos, por eso no puede ser parte del dato.
        if (valor == null || valor.isBlank() || valor.contains("|")) {
            throw new IllegalArgumentException(nombre + " no puede estar vacío ni contener '|'." );
        }
    }

    private static void validarNota(double valor) {
        // Este control se usa tanto al guardar una nota como al leerla del archivo.
        // Acepto solo notas finitas de 0 a 5 y con máximo un decimal.
        if (!Double.isFinite(valor) || valor < 0 || valor > 5
                || Math.abs(valor * 10 - Math.rint(valor * 10)) > 1e-8) {
            throw new IllegalArgumentException(
                    "La nota debe estar entre 0.0 y 5.0 y tener máximo un decimal.");
        }
    }

    private static Path rutaArchivoPorDefecto() {
        Path directorioActual = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        // Si ejecuté Maven desde el módulo, los datos quedan en esa carpeta.
        if (Files.isRegularFile(directorioActual.resolve("pom.xml"))) {
            return directorioActual.resolve("notas.txt");
        }

        Path carpetaProyecto = directorioActual.resolve("notascurso");
        // También detecto cuando el programa se inició desde la raíz del repositorio.
        if (Files.isRegularFile(carpetaProyecto.resolve("pom.xml"))) {
            return carpetaProyecto.resolve("notas.txt");
        }

        return directorioActual.resolve("notas.txt");
    }
}
