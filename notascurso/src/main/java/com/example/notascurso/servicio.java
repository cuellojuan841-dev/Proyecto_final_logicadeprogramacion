package com.example.notascurso;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class servicio {

    public cuentaUsuario autenticar(String usuario, String contraseña) {
        List<cuentaUsuario> usuarios = cargarUsuarios();
        for (cuentaUsuario u : usuarios) {
            if (u.getUsuario().equals(usuario) && u.getContraseña().equals(contraseña)) {
                return u;
            }
        }
        return null;
    }

    public List<cuentaUsuario> cargarUsuarios() {
        List<cuentaUsuario> usuarios = new ArrayList<>();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("usuarios.txt")) {
            if (input == null) {
                throw new IOException("No se encontró el archivo usuarios.txt dentro de src/main/resources");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    if (linea.trim().isEmpty()) {
                        continue;
                    }

                    String[] datos = linea.split(";");
                    cuentaUsuario usuario = new cuentaUsuario();
                    usuario.setUsuario(datos[0]);
                    usuario.setContraseña(datos[1]);
                    usuario.setNombre(datos[2]);
                    usuario.setRole(datos[3]);

                    List<String> materias = new ArrayList<>();
                    String[] asignaturas = datos[4].split(",");
                    for (String asignatura : asignaturas) {
                        materias.add(asignatura.trim());
                    }
                    usuario.setObjetos(materias);
                    usuarios.add(usuario);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return usuarios;
    }
}
