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
        Asignatura asignatura = new Asignatura(nombre);
        asignatura.setActividades(actividadesDeAsignatura(nombre));
        return asignatura;
    }

    public List<ActividadEvaluativa> actividadesDeAsignatura(String asignatura)
            throws IOException {
        validarTexto(asignatura, "La asignatura");
        List<ActividadEvaluativa> actividades = new ArrayList<>();

        if (Files.exists(archivoPlanes)) {
            List<String> lineas = Files.readAllLines(archivoPlanes, StandardCharsets.UTF_8);
            for (int indice = 0; indice < lineas.size(); indice++) {
                String linea = lineas.get(indice).trim();
                if (linea.isEmpty()) {
                    continue;
                }

                String[] datos = linea.split("\\|", -1);
                if (datos.length != 3) {
                    throw new IOException("Formato inválido en planes.txt, línea "
                            + (indice + 1));
                }

                if (datos[0].trim().equals(asignatura)) {
                    try {
                        String nombre = datos[1].trim();
                        double porcentaje = Double.parseDouble(datos[2].trim());
                        validarActividad(nombre, porcentaje);
                        actividades.add(new ActividadEvaluativa(nombre, porcentaje));
                    } catch (IllegalArgumentException e) {
                        throw new IOException("Dato inválido en planes.txt, línea "
                                + (indice + 1), e);
                    }
                }
            }
        }

        if (actividades.isEmpty()) {
            actividades.add(new ActividadEvaluativa("Período 1", 30));
            actividades.add(new ActividadEvaluativa("Período 2", 30));
            actividades.add(new ActividadEvaluativa("Período 3", 40));
        }
        return actividades;
    }

    public void guardarPlanEvaluacion(String asignatura,
            List<ActividadEvaluativa> actividades) throws IOException {
        validarTexto(asignatura, "La asignatura");
        if (actividades == null || actividades.isEmpty()) {
            throw new IllegalArgumentException("El plan debe tener al menos una actividad.");
        }

        double suma = 0;
        for (int indice = 0; indice < actividades.size(); indice++) {
            ActividadEvaluativa actividad = actividades.get(indice);
            validarActividad(actividad.getNombre(), actividad.getPorcentaje());
            suma += actividad.getPorcentaje();

            for (int anterior = 0; anterior < indice; anterior++) {
                if (actividades.get(anterior).getNombre()
                        .equalsIgnoreCase(actividad.getNombre().trim())) {
                    throw new IllegalArgumentException(
                            "No repitas el nombre de una actividad.");
                }
            }
        }

        if (Math.abs(suma - 100.0) > 1e-8) {
            throw new IllegalArgumentException(
                    "Los porcentajes de las actividades deben sumar exactamente 100%.");
        }

        List<Nota> notas = leerTodas();
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

        for (ActividadEvaluativa actividad : actividades) {
            lineas.add(asignatura + "|" + actividad.getNombre().trim()
                    + "|" + actividad.getPorcentaje());
        }

        Path carpeta = archivoPlanes.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        Files.write(archivoPlanes, lineas, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public List<Nota> obtenerNotas(String asignatura) throws IOException {
        List<Nota> resultado = new ArrayList<>();
        for (Nota nota : leerTodas()) {
            if (nota.getAsignatura().equals(asignatura)) {
                resultado.add(nota);
            }
        }
        return resultado;
    }

    public List<Nota> obtenerNotasEstudiante(String usuario) throws IOException {
        List<Nota> resultado = new ArrayList<>();
        for (Nota nota : leerTodas()) {
            if (nota.getUsuarioEstudiante().equals(usuario)) {
                resultado.add(nota);
            }
        }
        return resultado;
    }

    public Double obtenerNota(String usuario, String asignatura, int periodo) throws IOException {
        for (Nota nota : leerTodas()) {
            if (coincide(nota, usuario, asignatura, periodo)) {
                return nota.getValor();
            }
        }
        return null;
    }

    public void guardarNota(String usuario, String asignatura, int periodo, double valor)
            throws IOException {
        validarTexto(usuario, "El usuario del estudiante");
        validarTexto(asignatura, "La asignatura");
        validarNota(valor);

        List<ActividadEvaluativa> actividades = actividadesDeAsignatura(asignatura);
        if (periodo < 1 || periodo > actividades.size()) {
            throw new IllegalArgumentException(
                    "Selecciona una actividad que exista en el plan de evaluación.");
        }

        List<Nota> notas = leerTodas();
        List<Nota> notasActualizadas = new ArrayList<>();
        for (Nota nota : notas) {
            if (!coincide(nota, usuario, asignatura, periodo)) {
                notasActualizadas.add(nota);
            }
        }
        notasActualizadas.add(new Nota(usuario, asignatura, periodo, valor));
        guardarTodas(notasActualizadas);
    }

    public boolean eliminarNota(String usuario, String asignatura, int periodo) throws IOException {
        if (periodo < 1) {
            throw new IllegalArgumentException("La actividad debe ser un número positivo.");
        }
        List<Nota> notas = leerTodas();
        boolean eliminada = false;
        for (int indice = notas.size() - 1; indice >= 0; indice--) {
            Nota nota = notas.get(indice);
            if (coincide(nota, usuario, asignatura, periodo)) {
                notas.remove(indice);
                eliminada = true;
            }
        }
        if (eliminada) {
            guardarTodas(notas);
        }
        return eliminada;
    }

    public Double promedioActual(String usuario, String asignatura) throws IOException {
        List<Nota> notas = notasDeEstudianteYMateria(leerTodas(), usuario, asignatura);
        double puntos = 0;
        double pesoCalificado = 0;

        for (Nota nota : notas) {
            double peso = pesoActividad(asignatura, nota.getPeriodo());
            puntos += nota.getValor() * peso;
            pesoCalificado += peso;
        }

        if (pesoCalificado == 0) {
            return null;
        }
        return puntos / pesoCalificado;
    }

    public double puntosAcumulados(String usuario, String asignatura) throws IOException {
        double puntos = 0;
        for (Nota nota : notasDeEstudianteYMateria(leerTodas(), usuario, asignatura)) {
            puntos += nota.getValor() * pesoActividad(asignatura, nota.getPeriodo());
        }
        return puntos;
    }

    public double porcentajePendiente(String usuario, String asignatura) throws IOException {
        List<ActividadEvaluativa> actividades = actividadesDeAsignatura(asignatura);
        List<Nota> notas = notasDeEstudianteYMateria(leerTodas(), usuario, asignatura);
        double pendiente = 0;

        for (int indice = 0; indice < actividades.size(); indice++) {
            boolean calificada = false;
            for (Nota nota : notas) {
                if (nota.getPeriodo() == indice + 1) {
                    calificada = true;
                }
            }
            if (!calificada) {
                pendiente += actividades.get(indice).getPorcentaje() / 100.0;
            }
        }
        return pendiente;
    }

    private double pesoActividad(String asignatura, int numeroActividad) throws IOException {
        List<ActividadEvaluativa> actividades = actividadesDeAsignatura(asignatura);
        if (numeroActividad < 1 || numeroActividad > actividades.size()) {
            throw new IOException("La nota guardada hace referencia a una actividad "
                    + "que no existe en el plan de " + asignatura + ".");
        }
        return actividades.get(numeroActividad - 1).getPorcentaje() / 100.0;
    }

    private List<Nota> leerTodas() throws IOException {
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }

        List<Nota> notas = new ArrayList<>();
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        for (int indice = 0; indice < lineas.size(); indice++) {
            String linea = lineas.get(indice).trim();
            if (linea.isEmpty()) {
                continue;
            }

            String[] datos = linea.split("\\|", -1);
            if (datos.length != 4) {
                throw new IOException("Formato inválido en notas.txt, línea " + (indice + 1));
            }

            try {
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
                throw new IOException("Dato inválido en notas.txt, línea " + (indice + 1), e);
            }
        }
        return notas;
    }

    private void guardarTodas(List<Nota> notas) throws IOException {
        Path carpeta = archivo.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }

        List<String> lineas = new ArrayList<>();
        for (Nota nota : notas) {
            lineas.add(nota.getUsuarioEstudiante() + "|"
                    + nota.getAsignatura() + "|"
                    + nota.getPeriodo() + "|"
                    + nota.getValor());
        }

        Files.write(archivo, lineas, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static List<Nota> notasDeEstudianteYMateria(
            List<Nota> notas, String usuario, String asignatura) {
        List<Nota> resultado = new ArrayList<>();
        for (Nota nota : notas) {
            if (nota.getUsuarioEstudiante().equals(usuario)
                    && nota.getAsignatura().equals(asignatura)) {
                resultado.add(nota);
            }
        }
        return resultado;
    }

    private static boolean coincide(Nota nota, String usuario, String asignatura, int periodo) {
        return nota.getUsuarioEstudiante().equals(usuario)
                && nota.getAsignatura().equals(asignatura)
                && nota.getPeriodo() == periodo;
    }

    private static void validarActividad(String nombre, double porcentaje) {
        validarTexto(nombre, "El nombre de la actividad");
        if (nombre.contains("\n") || nombre.contains("\r")) {
            throw new IllegalArgumentException("El nombre no puede tener saltos de línea.");
        }
        if (!Double.isFinite(porcentaje) || porcentaje <= 0 || porcentaje > 100) {
            throw new IllegalArgumentException(
                    "Cada porcentaje debe ser mayor que 0 y máximo 100.");
        }
    }

    private static void validarTexto(String valor, String nombre) {
        if (valor == null || valor.isBlank() || valor.contains("|")) {
            throw new IllegalArgumentException(nombre + " no puede estar vacío ni contener '|'." );
        }
    }

    private static void validarNota(double valor) {
        if (!Double.isFinite(valor) || valor < 0 || valor > 5
                || Math.abs(valor * 10 - Math.rint(valor * 10)) > 1e-8) {
            throw new IllegalArgumentException(
                    "La nota debe estar entre 0.0 y 5.0 y tener máximo un decimal.");
        }
    }

    private static Path rutaArchivoPorDefecto() {
        Path directorioActual = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (Files.isRegularFile(directorioActual.resolve("pom.xml"))) {
            return directorioActual.resolve("notas.txt");
        }

        Path carpetaProyecto = directorioActual.resolve("notascurso");
        if (Files.isRegularFile(carpetaProyecto.resolve("pom.xml"))) {
            return carpetaProyecto.resolve("notas.txt");
        }

        return directorioActual.resolve("notas.txt");
    }
}
