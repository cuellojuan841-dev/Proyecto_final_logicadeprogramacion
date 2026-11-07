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
        List<cuentaUsuario> usuarios = cargarUsuarios();
        for (cuentaUsuario u : usuarios) {
            if (u.getUsuario().equals(usuario) && u.getContraseña().equals(contraseña)) {
                VaadinSession session = VaadinSession.getCurrent();
                if (session != null) {
                    session.setAttribute(cuentaUsuario.class, u);
                }
                return u;
            }
        }
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(cuentaUsuario.class, null);
        }
        return null;
    }

    public cuentaUsuario getUsuarioActual() {
        VaadinSession session = VaadinSession.getCurrent();
        return session == null ? null : session.getAttribute(cuentaUsuario.class);
    }

    public List<cuentaUsuario> cargarUsuarios() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("usuarios.txt");
        if (input == null) {
            throw new IOException(
                    "No se encontró usuarios.txt dentro de src/main/resources.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return leerUsuarios(reader);
        }
    }

    static List<cuentaUsuario> leerUsuarios(BufferedReader reader) throws IOException {
        List<cuentaUsuario> usuarios = new ArrayList<>();
        String linea;
        int numeroLinea = 0;

        while ((linea = reader.readLine()) != null) {
            numeroLinea++;
            if (linea.isBlank()) {
                continue;
            }

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

            if (nombreUsuario.isEmpty() || contraseña.isBlank() || nombre.isEmpty()
                    || rol.isEmpty() || asignaturasTexto.isEmpty()) {
                throw errorFormato(numeroLinea, "hay campos obligatorios vacíos.");
            }
            if (!rol.equalsIgnoreCase("PROFESOR")
                    && !rol.equalsIgnoreCase("ESTUDIANTE")
                    && !rol.equalsIgnoreCase("ROLE_PROFESOR")
                    && !rol.equalsIgnoreCase("ROLE_ESTUDIANTE")) {
                throw errorFormato(numeroLinea, "el rol no es PROFESOR ni ESTUDIANTE.");
            }

            List<String> materias = new ArrayList<>();
            for (String asignatura : asignaturasTexto.split(",", -1)) {
                String materia = asignatura.trim();
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
            usuarios.add(usuario);
        }

        return usuarios;
    }

    private static IOException errorFormato(int numeroLinea, String detalle) {
        return new IOException("Formato inválido en usuarios.txt, línea "
                + numeroLinea + ": " + detalle);
    }
}
