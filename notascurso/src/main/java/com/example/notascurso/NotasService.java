package com.example.notascurso;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

public class NotasService {
    private static final double[] PESOS = {0.30, 0.30, 0.40};
    private final Path archivo;

    public NotasService() {
        this(rutaArchivoPorDefecto());
    }

    public NotasService(Path archivo) {
        this.archivo = archivo;
    }

    public List<Nota> obtenerNotas(String asignatura) throws IOException {
        return leerTodas().stream()
                .filter(nota -> nota.getAsignatura().equals(asignatura))
                .toList();
    }

    public List<Nota> obtenerNotasEstudiante(String usuario) throws IOException {
        return leerTodas().stream()
                .filter(nota -> nota.getUsuarioEstudiante().equals(usuario))
                .toList();
    }

    public Double obtenerNota(String usuario, String asignatura, int periodo) throws IOException {
        return leerTodas().stream()
                .filter(nota -> coincide(nota, usuario, asignatura, periodo))
                .map(Nota::getValor)
                .findFirst()
                .orElse(null);
    }

    public void guardarNota(String usuario, String asignatura, int periodo, double valor)
            throws IOException {
        validarTexto(usuario, "El usuario del estudiante");
        validarTexto(asignatura, "La asignatura");
        validarPeriodo(periodo);
        validarNota(valor);

        List<Nota> notas = leerTodas();
        notas.removeIf(nota -> coincide(nota, usuario, asignatura, periodo));
        notas.add(new Nota(usuario, asignatura, periodo, valor));
        guardarTodas(notas);
    }

    public boolean eliminarNota(String usuario, String asignatura, int periodo) throws IOException {
        validarPeriodo(periodo);
        List<Nota> notas = leerTodas();
        boolean eliminada = notas.removeIf(nota -> coincide(nota, usuario, asignatura, periodo));
        if (eliminada) {
            guardarTodas(notas);
        }
        return eliminada;
    }

    public OptionalDouble promedioActual(String usuario, String asignatura) throws IOException {
        List<Nota> notas = notasDeEstudianteYMateria(leerTodas(), usuario, asignatura);
        double puntos = 0;
        double pesoCalificado = 0;

        for (Nota nota : notas) {
            double peso = PESOS[nota.getPeriodo() - 1];
            puntos += nota.getValor() * peso;
            pesoCalificado += peso;
        }

        if (pesoCalificado == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(puntos / pesoCalificado);
    }

    public double puntosAcumulados(String usuario, String asignatura) throws IOException {
        double puntos = 0;
        for (Nota nota : notasDeEstudianteYMateria(leerTodas(), usuario, asignatura)) {
            puntos += nota.getValor() * PESOS[nota.getPeriodo() - 1];
        }
        return puntos;
    }

    public double porcentajePendiente(String usuario, String asignatura) throws IOException {
        double pesoCalificado = 0;
        for (Nota nota : notasDeEstudianteYMateria(leerTodas(), usuario, asignatura)) {
            pesoCalificado += PESOS[nota.getPeriodo() - 1];
        }
        return 1.0 - pesoCalificado;
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
                validarPeriodo(periodo);
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

        List<String> lineas = notas.stream()
                .sorted(Comparator.comparing(Nota::getUsuarioEstudiante)
                        .thenComparing(Nota::getAsignatura)
                        .thenComparingInt(Nota::getPeriodo))
                .map(nota -> String.join("|",
                        nota.getUsuarioEstudiante(),
                        nota.getAsignatura(),
                        Integer.toString(nota.getPeriodo()),
                        Double.toString(nota.getValor())))
                .toList();

        Files.write(archivo, lineas, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static List<Nota> notasDeEstudianteYMateria(
            List<Nota> notas, String usuario, String asignatura) {
        return notas.stream()
                .filter(nota -> nota.getUsuarioEstudiante().equals(usuario)
                        && nota.getAsignatura().equals(asignatura))
                .toList();
    }

    private static boolean coincide(Nota nota, String usuario, String asignatura, int periodo) {
        return nota.getUsuarioEstudiante().equals(usuario)
                && nota.getAsignatura().equals(asignatura)
                && nota.getPeriodo() == periodo;
    }

    private static void validarTexto(String valor, String nombre) {
        if (valor == null || valor.isBlank() || valor.contains("|")) {
            throw new IllegalArgumentException(nombre + " no puede estar vacío ni contener '|'.");
        }
    }

    private static void validarPeriodo(int periodo) {
        if (periodo < 1 || periodo > 3) {
            throw new IllegalArgumentException("El período debe ser 1, 2 o 3.");
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
