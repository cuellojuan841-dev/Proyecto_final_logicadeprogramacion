package com.example.notascurso;

import com.vaadin.flow.server.VaadinSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class servicio {
    public cuentaUsuario autenticar(String usuario, String contraseña) throws IOException {
        // Primero leo las cuentas del archivo; si está mal formado, cargarUsuarios informa el error.
        List<cuentaUsuario> usuarios = cargarUsuarios();
        // Comparo los datos ingresados con cada usuario hasta hallar una coincidencia exacta.
        for (cuentaUsuario u : usuarios) {
            if (u.getUsuario().equals(usuario) && u.getContraseña().equals(contraseña)) {
                VaadinSession session = VaadinSession.getCurrent();
                // Guardo la cuenta en la sesión para que las vistas conozcan identidad y rol.
                if (session != null) {
                    session.setAttribute(cuentaUsuario.class, u);
                }
                return u;
            }
        }
        // Si nadie coincidió, quito cualquier cuenta anterior para no conservar una sesión vieja.
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(cuentaUsuario.class, null);
        }
        return null;
    }

    public cuentaUsuario getUsuarioActual() {
        VaadinSession session = VaadinSession.getCurrent();
        // Devuelvo la cuenta guardada o null si todavía no existe una sesión activa.
        return session == null ? null : session.getAttribute(cuentaUsuario.class);
    }

    public List<cuentaUsuario> cargarUsuarios() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("usuarios.txt");
        // Sin el archivo no puedo autenticar, así que aviso en vez de devolver datos vacíos.
        if (input == null) {
            throw new IOException(
                    "No se encontró usuarios.txt dentro de src/main/resources.");
        }

        // El try con recursos cierra el lector incluso si el formato del archivo da error.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return leerUsuarios(reader);
        }
    }

    static List<cuentaUsuario> leerUsuarios(BufferedReader reader) throws IOException {
        List<cuentaUsuario> usuarios = new ArrayList<>();
        String linea;
        int numeroLinea = 0;

        // Cada línea representa una cuenta; el ciclo termina cuando ya no quedan líneas.
        while ((linea = reader.readLine()) != null) {
            numeroLinea++;
            // Las líneas vacías no contienen cuentas, por eso paso a la siguiente.
            if (linea.isBlank()) {
                continue;
            }

            // Cada cuenta debe tener cinco partes separadas por |.
            String[] datos = linea.split("\\|", -1);
            if (datos.length != 5) {
                throw errorFormato(numeroLinea,
                        "se esperaban 5 campos separados por '|'.");
            }

            String nombreUsuario = datos[0].trim();
            String contraseña = datos[1];
            String nombre = datos[2].trim();
            String rol = datos[3].trim();
            String asignaturasTexto = datos[4].trim();

            // Reviso que los campos obligatorios tengan contenido.
            if (nombreUsuario.isEmpty() || contraseña.isBlank() || nombre.isEmpty()
                    || rol.isEmpty() || asignaturasTexto.isEmpty()) {
                throw errorFormato(numeroLinea, "hay campos obligatorios vacíos.");
            }
            // Solo acepto los dos roles que tienen vistas en la aplicación.
            if (!rol.equalsIgnoreCase("PROFESOR")
                    && !rol.equalsIgnoreCase("ESTUDIANTE")
                    && !rol.equalsIgnoreCase("ROLE_PROFESOR")
                    && !rol.equalsIgnoreCase("ROLE_ESTUDIANTE")) {
                throw errorFormato(numeroLinea, "el rol no es PROFESOR ni ESTUDIANTE.");
            }

            List<String> materias = new ArrayList<>();
            // Separo las asignaturas por coma para guardarlas como una lista en la cuenta.
            for (String asignatura : asignaturasTexto.split(",", -1)) {
                String materia = asignatura.trim();
                // Rechazo comas seguidas o al final porque dejan una asignatura vacía.
                if (materia.isEmpty()) {
                    throw errorFormato(numeroLinea,
                            "hay una asignatura vacía en la lista.");
                }
                materias.add(materia);
            }

            cuentaUsuario usuario = new cuentaUsuario();
            usuario.setUsuario(nombreUsuario);
            usuario.setContraseña(contraseña);
            usuario.setNombre(nombre);
            usuario.setRole(rol);
            usuario.setObjetos(materias);
            // Solo agrego la cuenta cuando todos sus campos ya pasaron las validaciones.
            usuarios.add(usuario);
        }

        return usuarios;
    }

    private static IOException errorFormato(int numeroLinea, String detalle) {
        // Incluyo el número de línea para ubicar más fácil el dato incorrecto.
        return new IOException("Formato inválido en usuarios.txt, línea "
                + numeroLinea + ": " + detalle);
    }
}
